package com.chuckerteam.chucker.api

import android.content.Context
import com.chuckerteam.chucker.internal.data.entity.HttpHeader
import com.chuckerteam.chucker.internal.data.entity.HttpTransaction
import com.chuckerteam.chucker.internal.support.JsonConverter
import com.google.gson.reflect.TypeToken
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.ForwardingClientCallListener
import io.grpc.Metadata
import io.grpc.MethodDescriptor
import io.grpc.Status
import java.nio.charset.Charset
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

public class ChuckerGrpcInterceptor private constructor(
    private val collector: ChuckerCollector,
    private val maxContentLength: Long = 250_000L, // Default max content length
    private val headersToRedact: Set<String> = emptySet()
) : ClientInterceptor {

    public constructor(
        collector: ChuckerCollector,
        context: Context,
        maxContentLength: Long = 250_000L,
        redactHeaders: Set<String> = emptySet()
    ) : this(collector, maxContentLength, redactHeaders.map { it.lowercase() }.toSet())

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        val transaction = HttpTransaction()
        transaction.id = UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE // Crude ID generation
        transaction.requestDate = System.currentTimeMillis()
        transaction.method = method.type.name // e.g., UNARY, CLIENT_STREAMING
        transaction.protocol = "gRPC"// Or extract from method if possible

        val authority = next.authority() ?: "unknown_authority"
        transaction.host = authority.substringBefore(":")
        // val port = authority.substringAfter(":", "") // Unused
        transaction.path = "/${method.fullMethodName}"
        transaction.url = "${determineScheme(authority)}://$authority${transaction.path}"
        transaction.scheme = determineScheme(authority)

        val requestBodyBuilder = StringBuilder()
        val requestBodySize = AtomicLong(0L)
        val responseBodyBuilder = StringBuilder()
        val responseBodySize = AtomicLong(0L)
        val requestSent = AtomicBoolean(false)

        val originalCall = next.newCall(method, callOptions)

        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(originalCall) {
            override fun start(responseListener: Listener<RespT>, headers: Metadata) {
                // Use the member function from HttpTransaction
                transaction.setRequestHeaders(convertMetadataToHeaders(headers, headersToRedact))
                transaction.requestContentType = "application/grpc"

                val newListener = object : ForwardingClientCallListener.SimpleForwardingClientCallListener<RespT>(responseListener) {
                    override fun onHeaders(responseHeaders: Metadata) {
                        transaction.responseDate = System.currentTimeMillis()
                        // Use the member function from HttpTransaction
                        transaction.setResponseHeaders(convertMetadataToHeaders(responseHeaders, headersToRedact))
                        transaction.responseContentType = "application/grpc"
                        super.onHeaders(responseHeaders)
                    }

                    override fun onMessage(message: RespT) {
                        val messageString = messageToString(message)
                        if (responseBodySize.get() < maxContentLength) {
                            responseBodyBuilder.append(messageString)
                        }
                        responseBodySize.addAndGet(messageString.length.toLong())
                        super.onMessage(message)
                    }

                    override fun onClose(status: Status, trailers: Metadata) {
                        transaction.responseCode = status.code.value()
                        transaction.responseMessage = status.code.name
                        if (status.description != null) {
                            transaction.responseMessage += " (${status.description})"
                        }
                        if (status.cause != null) {
                            transaction.error = status.cause?.toString()
                        }
                        transaction.setResponseTrailers(convertMetadataToHeaders(trailers, headersToRedact))

                        transaction.tookMs = System.currentTimeMillis() - (transaction.requestDate ?: System.currentTimeMillis())

                        transaction.requestBody = requestBodyBuilder.toString()
                        transaction.requestPayloadSize = requestBodySize.get()
                        if (requestBodySize.get() > maxContentLength) {
                             transaction.requestBody = transaction.requestBody?.take(maxContentLength.toInt()) + "... (truncated)"
                        }

                        transaction.responseBody = responseBodyBuilder.toString()
                        transaction.responsePayloadSize = responseBodySize.get()
                        if (responseBodySize.get() > maxContentLength) {
                            transaction.responseBody = transaction.responseBody?.take(maxContentLength.toInt()) + "... (truncated)"
                        }

                        if (!requestSent.getAndSet(true)) {
                             collector.onRequestSent(transaction)
                        }
                        collector.onResponseReceived(transaction)
                        super.onClose(status, trailers)
                    }
                }
                super.start(newListener, headers)
            }

            override fun sendMessage(message: ReqT) {
                if (requestBodySize.get() == 0L) {
                    val messageString = messageToString(message)
                     if (requestBodySize.get() < maxContentLength) {
                        requestBodyBuilder.append(messageString)
                    }
                    requestBodySize.addAndGet(messageString.length.toLong())
                }

                if (!requestSent.getAndSet(true)) {
                    transaction.requestBody = requestBodyBuilder.toString()
                    transaction.requestPayloadSize = requestBodySize.get()
                    collector.onRequestSent(transaction)
                }
                super.sendMessage(message)
            }
        }
    }

    private fun determineScheme(authority: String): String {
        return if (authority.contains(":") && authority.substringAfterLast(":") == "443") "https" else "http"
    }

    private fun convertMetadataToHeaders(metadata: Metadata, headersToRedact: Set<String>): List<HttpHeader> {
        val headers = mutableListOf<HttpHeader>()
        metadata.keys().forEach { keyName ->
            val isBinary = keyName.endsWith(Metadata.BINARY_HEADER_SUFFIX)
            val values: Iterable<String>? = if (isBinary) {
                 metadata.getAll(Metadata.Key.of(keyName, Metadata.BINARY_BYTE_MARSHALLER))
                    ?.map { bytes -> bytes.toString(Charset.forName("UTF-8")) + " (binary)" }
            } else {
                 metadata.getAll(Metadata.Key.of(keyName, Metadata.ASCII_STRING_MARSHALLER))
            }

            values?.forEach { value ->
                val displayValue = if (headersToRedact.contains(keyName.lowercase())) "**REDACTED**" else value
                headers.add(HttpHeader(keyName, displayValue))
            }
        }
        return headers
    }

    private fun <T> messageToString(message: T?): String {
        return message?.toString() ?: ""
    }

    private fun HttpTransaction.setResponseTrailers(headers: List<HttpHeader>) {
        if (headers.isNotEmpty()) {
            val typeToken = object : TypeToken<List<HttpHeader>>() {}.type
            val existingHeadersJson = this.responseHeaders ?: "[]"
            // Assuming JsonConverter.instance.fromJson can take typeToken
            val existingHeaders = JsonConverter.instance.fromJson<List<HttpHeader>>(existingHeadersJson, typeToken) ?: emptyList()
            val allHeaders = existingHeaders.toMutableList()
            headers.forEach { trailer -> allHeaders.add(HttpHeader("(Trailer) ${trailer.name}", trailer.value)) }
            this.responseHeaders = JsonConverter.instance.toJson(allHeaders)
        }
    }
}
