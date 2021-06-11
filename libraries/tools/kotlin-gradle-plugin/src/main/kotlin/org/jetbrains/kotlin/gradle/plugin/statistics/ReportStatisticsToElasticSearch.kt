/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.statistics

import com.google.gson.Gson
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties
import org.jetbrains.kotlin.cli.common.toBooleanLenient
import org.jetbrains.kotlin.gradle.plugin.stat.ReportStatistics
import org.jetbrains.kotlin.gradle.plugin.stat.CompileStatData
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

object ReportStatisticsToElasticSearch : ReportStatistics {
    val url by lazy { CompilerSystemProperties.KOTLIN_STAT_ENDPOINT_PROPERTY.value }
    val user by lazy { CompilerSystemProperties.KOTLIN_STAT_USER_PROPERTY.value }
    val enable: Boolean by lazy { CompilerSystemProperties.KOTLIN_STAT_ENABLED_PROPERTY.value?.toBooleanLenient() ?: false }

    //TODO Do not store password as string
    val password by lazy { CompilerSystemProperties.KOTLIN_STAT_PASSWORD_PROPERTY.value }

    override fun report(data: CompileStatData) {
        if (!enable) {
            return;
        }

        val connection = URL(url).openConnection() as HttpURLConnection

        try {
            if (user != null && password != null) {
                val auth = Base64.getEncoder()
                    .encode("$user:$password".toByteArray()).toString(Charsets.UTF_8)
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

    private fun checkResponseAndLog(connection: HttpURLConnection) {
        val isResponseBad = connection.responseCode !in 200..299
        if (isResponseBad) {
            throw Exception(
                "Failed to send statistic to ${connection.url}: ${connection.responseMessage}"
            )
        }
    }
}
