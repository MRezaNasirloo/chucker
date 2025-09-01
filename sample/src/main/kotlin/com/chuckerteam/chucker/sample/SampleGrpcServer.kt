package com.chuckerteam.chucker.sample

import android.util.Log
import com.chuckerteam.chucker.sample.grpc.HelloReply
import com.chuckerteam.chucker.sample.grpc.HelloRequest
import com.chuckerteam.chucker.sample.grpc.SampleGreeterGrpc
import io.grpc.Server
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import java.io.IOException
import java.util.concurrent.TimeUnit

class SampleGrpcServer(private val port: Int) {

    private val server: Server by lazy {
        NettyServerBuilder.forPort(port)
            .addService(GreeterService())
            .executor(Dispatchers.IO.asExecutor()) // Offload calls from the main thread
            .build()
    }

    fun start() {
        try {
            if (server.isShutdown || server.isTerminated) {
                 // This basic server isn't designed to be restarted once fully terminated.
                 // In a real app, you might need a new ServerImpl instance.
                 Log.e("SampleGrpcServer", "Server was shutdown and cannot be restarted.")
                 return
            }
            server.start()
            Log.i("SampleGrpcServer", "Server started, listening on $port")
        } catch (e: IOException) {
            Log.e("SampleGrpcServer", "Server start failed", e)
        }
    }

    fun stop() {
        Log.i("SampleGrpcServer", "Attempting to shut down gRPC server...")
        try {
            server.shutdown().awaitTermination(5, TimeUnit.SECONDS)
            Log.i("SampleGrpcServer", "Server shutdown gracefully.")
        } catch (e: InterruptedException) {
            Log.w("SampleGrpcServer", "Server shutdown interrupted, forcing shutdown.", e)
            server.shutdownNow()
            Thread.currentThread().interrupt() // Preserve interrupt status
        } catch (e: Exception) { // Catch any other unexpected errors during shutdown
            Log.e("SampleGrpcServer", "Error during server shutdown", e)
            server.shutdownNow() // Ensure it's shut down
        }
        Log.i("SampleGrpcServer", "Server stopped. Is shutdown: ${server.isShutdown}, Is terminated: ${server.isTerminated}")
    }

    private class GreeterService : SampleGreeterGrpc.SampleGreeterImplBase() {
        override fun sayHello(req: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
            Log.i("GreeterService", "SayHello called with name: ${req.name}")
            val reply = HelloReply.newBuilder().setMessage("Hello ${req.name} from server (Unary)").build()
            responseObserver.onNext(reply)
            responseObserver.onCompleted()
        }

        override fun sayHelloServerStream(req: HelloRequest, responseObserver: StreamObserver<HelloReply>) {
            Log.i("GreeterService", "SayHelloServerStream called with name: ${req.name}")
            for (i in 1..3) { // Send a few messages for streaming
                val reply = HelloReply.newBuilder().setMessage("Hello ${req.name}, part $i (Server Stream)").build()
                responseObserver.onNext(reply)
                try {
                    Thread.sleep(300) // Simulate some work
                } catch (e: InterruptedException) {
                    Thread.currentThread().interrupt()
                    Log.w("GreeterService", "Server stream interrupted", e)
                    responseObserver.onError(io.grpc.Status.CANCELLED.withDescription("Stream interrupted by server").asRuntimeException())
                    return
                }
            }
            responseObserver.onCompleted()
        }

        override fun sayHelloClientStream(responseObserver: StreamObserver<HelloReply>): StreamObserver<HelloRequest> {
            Log.i("GreeterService", "SayHelloClientStream call started")
            val namesCollected = StringBuilder()
            return object : StreamObserver<HelloRequest> {
                override fun onNext(value: HelloRequest) {
                    Log.i("GreeterService", "ClientStream received: ${value.name}")
                    if (namesCollected.isNotEmpty()) {
                        namesCollected.append(", ")
                    }
                    namesCollected.append(value.name)
                }

                override fun onError(t: Throwable) {
                    Log.e("GreeterService", "ClientStream error from client", t)
                    responseObserver.onError(
                        io.grpc.Status.fromThrowable(t)
                            .withDescription("Error received from client stream: ${t.message}")
                            .asRuntimeException()
                    )
                }

                override fun onCompleted() {
                    Log.i("GreeterService", "ClientStream completed by client. Names: $namesCollected")
                    val reply = HelloReply.newBuilder().setMessage("Hello $namesCollected! (Client Stream)").build()
                    responseObserver.onNext(reply)
                    responseObserver.onCompleted()
                }
            }
        }

        override fun sayHelloBidiStream(responseObserver: StreamObserver<HelloReply>): StreamObserver<HelloRequest> {
            Log.i("GreeterService", "SayHelloBidiStream call started")
            return object : StreamObserver<HelloRequest> {
                override fun onNext(value: HelloRequest) {
                    Log.i("GreeterService", "BidiStream received from client: ${value.name}")
                    val reply = HelloReply.newBuilder().setMessage("Server acknowledges: ${value.name} (Bidi Stream)").build()
                    responseObserver.onNext(reply) // Echo back or send other messages
                }

                override fun onError(t: Throwable) {
                    Log.e("GreeterService", "BidiStream error from client", t)
                     responseObserver.onError(
                        io.grpc.Status.fromThrowable(t)
                            .withDescription("Error received from client in bidi stream: ${t.message}")
                            .asRuntimeException()
                    )
                }

                override fun onCompleted() {
                    Log.i("Greeter_Service", "BidiStream completed by client.")
                    responseObserver.onCompleted() // Server also completes its sending side
                }
            }
        }
    }
}
