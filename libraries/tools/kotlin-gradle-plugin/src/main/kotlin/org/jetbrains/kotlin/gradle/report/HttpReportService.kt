/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.google.gson.Gson
import org.gradle.api.Project
import org.gradle.api.logging.Logging
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.tooling.events.FinishEvent
import org.gradle.tooling.events.OperationCompletionListener
import org.gradle.tooling.events.task.TaskFinishEvent
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatisticsData
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinBuildStatListener.Companion.prepareData
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

abstract class HttpReportService : BuildService<HttpReportService.Parameters>,
    OperationCompletionListener, AutoCloseable {

    var executorService: ExecutorService = Executors.newSingleThreadExecutor()

    interface Parameters : BuildServiceParameters {
        var label: String?
        var uuid: String
        var projectName: String
        var httpSettings: HttpReportSettings
        var kotlinVersion: String
    }

    private val log = Logging.getLogger(this.javaClass)

    override fun onFinish(event: FinishEvent?) {
        if (event is TaskFinishEvent) {
            val data = prepareData(event, parameters.projectName, parameters.uuid, parameters.label, parameters.kotlinVersion)
            data?.also { executorService.submit { report(data) } }
        }
    }

    override fun close() {
        executorService.shutdown()
    }

    companion object {

        fun registerIfAbsent(project: Project, kotlinVersion: String): Provider<HttpReportService>? {
            val rootProject = project.gradle.rootProject
            val reportingSettings = reportingSettings(rootProject)

            return reportingSettings.httpReportSettings?.let { httpSettings ->
                project.gradle.sharedServices.registerIfAbsent(
                    "build_http_metric_service_${HttpReportService::class.java.classLoader.hashCode()}",
                    HttpReportService::class.java
                ) {
                    it.parameters.label = reportingSettings.buildReportLabel
                    it.parameters.projectName = rootProject.name
                    it.parameters.uuid = UUID.randomUUID().toString()
                    it.parameters.httpSettings = httpSettings
                    it.parameters.kotlinVersion = kotlinVersion
                }!!
            }

        }

    }

    fun report(data: CompileStatisticsData) {
        val elapsedTime = measureTimeMillis {
            val connection = URL(parameters.httpSettings.url).openConnection() as HttpURLConnection

            try {
                if (parameters.httpSettings.user != null && parameters.httpSettings.password != null) {
                    val auth = Base64.getEncoder()
                        .encode("${parameters.httpSettings.user}:${parameters.httpSettings.password}".toByteArray()).toString(Charsets.UTF_8)
                    connection.addRequestProperty("Authorization", "Basic $auth")
                }
                connection.addRequestProperty("Content-Type", "application/json")
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.outputStream.use {
                    it.write(Gson().toJson(data).toByteArray())
                }
                connection.connect()
                checkResponseAndLog(connection)
                connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
            } catch (e: Exception) {
                checkResponseAndLog(connection)
            } finally {
                connection.disconnect()
            }
        }
        log.debug("Report statistic by http takes $elapsedTime ms")
    }

    private fun checkResponseAndLog(connection: HttpURLConnection) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            log.warn("Failed to send statistic to ${connection.url}: ${connection.responseMessage}")
        }
    }

}
