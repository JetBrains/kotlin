/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.google.gson.Gson
import org.gradle.api.logging.Logging
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import org.jetbrains.kotlin.gradle.report.HttpReportSettings
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

class ReportStatisticsByHttp(
    private val httpProperties: HttpReportSettings
) : ReportStatistics {

    private val log = Logging.getLogger(this.javaClass)

    override fun report(data: CompileStatData) {
        val elapsedTime = measureTimeMillis {
            val connection = URL(httpProperties.url).openConnection() as HttpURLConnection

            try {
                if (httpProperties.user != null && httpProperties.password != null) {
                    val auth = Base64.getEncoder()
                        .encode("${httpProperties.user}:${httpProperties.password}".toByteArray()).toString(Charsets.UTF_8)
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
        log.debug("Report statistic to elastic search takes $elapsedTime ms")
    }

    private fun checkResponseAndLog(connection: HttpURLConnection) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            throw Exception(
                "Failed to send statistic to ${connection.url}: ${connection.responseMessage}"
            )
        }
    }
}
