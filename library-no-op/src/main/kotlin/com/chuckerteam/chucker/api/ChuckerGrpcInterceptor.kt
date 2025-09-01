package com.chuckerteam.chucker.api

import android.content.Context
import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.MethodDescriptor

/**
 * A no-op implementation of a gRPC interceptor that can be used in release builds.
 */
public class ChuckerGrpcInterceptor private constructor(
    // Parameters are kept for source compatibility with the library variant.
    @Suppress("UNUSED_PARAMETER") private val collector: ChuckerCollector,
    @Suppress("UNUSED_PARAMETER") private val maxContentLength: Long,
    @Suppress("UNUSED_PARAMETER") private val headersToRedact: Set<String>
) : ClientInterceptor {

    @Suppress("LongParameterList")
    public constructor(
        collector: ChuckerCollector,
        // The context is unused in the no-op version but kept for API compatibility.
        @Suppress("UNUSED_PARAMETER") context: Context,
        maxContentLength: Long = 250_000L,
        redactHeaders: Set<String> = emptySet()
    ) : this(collector, maxContentLength, redactHeaders)

    override fun <ReqT, RespT> interceptCall(
        method: MethodDescriptor<ReqT, RespT>,
        callOptions: CallOptions,
        next: Channel
    ): ClientCall<ReqT, RespT> {
        // In the no-op version, we just pass the call through without any interception.
        return next.newCall(method, callOptions)
    }
}
