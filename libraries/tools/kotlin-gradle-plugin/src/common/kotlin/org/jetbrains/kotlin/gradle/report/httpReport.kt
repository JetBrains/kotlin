/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.report

import com.google.gson.Gson
import org.gradle.api.logging.Logger
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.system.measureTimeMillis

private var invalidUrl = false
private var requestPreviousFailed = false

internal fun sendDataViaHttp(httpSettings: HttpReportSettings, data: Any, log: Logger) {
    val elapsedTime = measureTimeMillis {
        if (invalidUrl) {
            return
        }
        val connection = try {
            URL(httpSettings.url).openConnection() as HttpURLConnection
        } catch (e: IOException) {
            log.warn("Unable to open connection to ${httpSettings.url}: ${e.message}")
            invalidUrl = true
            return
        }

        try {
            if (httpSettings.user != null && httpSettings.password != null) {
                val auth = Base64.getEncoder()
                    .encode("${httpSettings.user}:${httpSettings.password}".toByteArray())
                    .toString(Charsets.UTF_8)
                connection.addRequestProperty("Authorization", "Basic $auth")
            }
            connection.addRequestProperty("Content-Type", "application/json")
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.outputStream.use {
                it.write(Gson().toJson(data).toByteArray())
            }
            connection.connect()
            checkResponseAndLog(connection, log)
        } catch (e: Exception) {
            log.debug("Unexpected exception happened ${e.message}: ${e.stackTrace}")
            checkResponseAndLog(connection, log)
        } finally {
            connection.disconnect()
        }
    }
    log.debug("Report statistic by http takes $elapsedTime ms")
}

private fun checkResponseAndLog(connection: HttpURLConnection, log: Logger) {
    val isResponseBad = connection.responseCode !in 200..299
    if (isResponseBad) {
        val message = "Failed to send statistic to ${connection.url} with ${connection.responseCode}: ${connection.responseMessage}"
        if (!requestPreviousFailed) {
            log.warn(message)
        } else {
            log.debug(message)
        }
        requestPreviousFailed = true
    }
}