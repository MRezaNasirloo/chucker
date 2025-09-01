
package com.chuckerteam.chucker.sample

import android.content.Context
import android.util.Log
import com.chuckerteam.chucker.api.ChuckerGrpcInterceptor
import com.chuckerteam.chucker.sample.grpc.HelloRequest
import com.chuckerteam.chucker.sample.grpc.SampleGreeterGrpc
import io.grpc.ManagedChannel
import io.grpc.okhttp.OkHttpChannelBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class GrpcTask(context: Context) {
    private val job = Job()
    private val scope = CoroutineScope(Dispatchers.IO + job)
    private val chuckerInterceptor = ChuckerGrpcInterceptor(
        (context.applicationContext as SampleApplication).chuckerCollector,
        context,
    )
    private lateinit var channel: ManagedChannel

    fun execute() {
        scope.launch {
            Log.i("GrpcTask", "Starting gRPC task")
            // Create a channel with the Chucker interceptor
            channel = OkHttpChannelBuilder.forAddress("localhost", 50051)
                .usePlaintext()
                .intercept(chuckerInterceptor)
                .build()

            val blockingStub = SampleGreeterGrpc.newBlockingStub(channel)
            val asyncStub = SampleGreeterGrpc.newStub(channel)

            // 1. Unary Call
            Log.i("GrpcTask", "Executing Unary call...")
            try {
                val unaryResponse = blockingStub.sayHello(HelloRequest.newBuilder().setName("UnaryUser").build())
                Log.i("GrpcTask", "Unary response: ${unaryResponse.message}")
            } catch (e: Exception) {
                Log.e("GrpcTask", "Unary call failed", e)
            }
            delay(500)

            // 2. Server Streaming Call
            Log.i("GrpcTask", "Executing Server Streaming call...")
            try {
                val streamResponse = blockingStub.sayHelloServerStream(HelloRequest.newBuilder().setName("ServerStreamUser").build())
                streamResponse.forEach { Log.i("GrpcTask", "Server stream response: ${it.message}") }
            } catch (e: Exception) {
                Log.e("GrpcTask", "Server stream call failed", e)
            }
            delay(500)

            // 3. Client Streaming Call
            Log.i("GrpcTask", "Executing Client Streaming call...")
            try {
                val finishLatch = CountDownLatch(1)
                val responseObserver = object : StreamObserver<com.chuckerteam.chucker.sample.grpc.HelloReply> {
                    override fun onNext(value: com.chuckerteam.chucker.sample.grpc.HelloReply) {
                        Log.i("GrpcTask", "Client stream response: ${value.message}")
                    }
                    override fun onError(t: Throwable) {
                        Log.e("GrpcTask", "Client stream failed", t)
                        finishLatch.countDown()
                    }
                    override fun onCompleted() {
                        Log.i("GrpcTask", "Client stream completed")
                        finishLatch.countDown()
                    }
                }
                val requestObserver = asyncStub.sayHelloClientStream(responseObserver)
                listOf("ClientStreamUser1", "ClientStreamUser2", "ClientStreamUser3").forEach {
                    requestObserver.onNext(HelloRequest.newBuilder().setName(it).build())
                    delay(200)
                }
                requestObserver.onCompleted()
                if (!finishLatch.await(10, TimeUnit.SECONDS)) {
                    Log.w("GrpcTask", "Client stream timed out")
                }
            } catch (e: Exception) {
                Log.e("GrpcTask", "Client stream call failed", e)
            }
            delay(500)

            // 4. Bi-directional Streaming Call
            Log.i("GrpcTask", "Executing Bi-directional Streaming call...")
            try {
                val finishLatchBidi = CountDownLatch(1)
                val responseObserverBidi = object : StreamObserver<com.chuckerteam.chucker.sample.grpc.HelloReply> {
                    override fun onNext(value: com.chuckerteam.chucker.sample.grpc.HelloReply) {
                        Log.i("GrpcTask", "Bidi stream response: ${value.message}")
                    }
                    override fun onError(t: Throwable) {
                        Log.e("GrpcTask", "Bidi stream failed", t)
                        finishLatchBidi.countDown()
                    }
                    override fun onCompleted() {
                        Log.i("GrpcTask", "Bidi stream server completed")
                        finishLatchBidi.countDown()
                    }
                }
                val requestObserverBidi = asyncStub.sayHelloBidiStream(responseObserverBidi)
                listOf("BidiUser1", "BidiUser2", "BidiUser3").asFlow().map {
                    delay(300)
                    HelloRequest.newBuilder().setName(it).build()
                }.collect { requestObserverBidi.onNext(it) }
                requestObserverBidi.onCompleted()
                if (!finishLatchBidi.await(10, TimeUnit.SECONDS)) {
                    Log.w("GrpcTask", "Bidi stream timed out")
                }
            } catch (e: Exception) {
                Log.e("GrpcTask", "Bidi stream call failed", e)
            }

            Log.i("GrpcTask", "Finished gRPC task, shutting down channel.")
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    fun cancel() {
        job.cancel()
        if (::channel.isInitialized && !channel.isTerminated) {
            channel.shutdownNow()
        }
    }
}
