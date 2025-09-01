
package com.chuckerteam.chucker.sample

import android.app.Application
import com.chuckerteam.chucker.api.ChuckerCollector
import com.chuckerteam.chucker.api.RetentionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SampleApplication : Application() {

    private val grpcServerPort = 50051 // Standard gRPC port
    private var sampleGrpcServer: SampleGrpcServer? = null
    private val serverJob = SupervisorJob()
    private val serverScope = CoroutineScope(Dispatchers.IO + serverJob)

    val chuckerCollector by lazy {
        ChuckerCollector(
            context = this,
            showNotification = true,
            retentionPeriod = RetentionManager.Period.ONE_HOUR,
        )
    }

    lateinit var grpcTask: GrpcTask
        private set

    override fun onCreate() {
        super.onCreate()
        grpcTask = GrpcTask(this)
        startGrpcServer()
    }

    private fun startGrpcServer() {
        serverScope.launch {
            if (sampleGrpcServer == null) {
                sampleGrpcServer = SampleGrpcServer(grpcServerPort)
            }
            sampleGrpcServer?.start()
        }
    }

    // This is less commonly used for a sample app, but good for completeness.
    // The server will be killed with the app process anyway.
    override fun onTerminate() {
        super.onTerminate()
        serverScope.launch {
            sampleGrpcServer?.stop()
        }
        serverJob.cancel()
        grpcTask.cancel()
    }
}
