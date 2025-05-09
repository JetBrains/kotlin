package org.jetbrains.kotlin.gradle.util

import io.ktor.http.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.test.fail

/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

fun awaitInitialization(port: Int, maxAttempts: Int = 20) {
    var attempts = 0
    val waitingTime = 500L
    while (initCall(port) != HttpStatusCode.OK.value) {
        attempts += 1
        if (attempts == maxAttempts) {
            fail("Failed to await server initialization for ${waitingTime * attempts}ms")
        }
        Thread.sleep(waitingTime)
    }
}

fun initCall(port: Int): Int {
    return try {
        val connection = URL("http://localhost:$port/isReady").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connect()
        connection.responseCode
    } catch (e: IOException) {
        fail("Unable to open connection: ${e.message}", e)
    }
}