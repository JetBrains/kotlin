/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package org.jetbrains.analyzer

import java.io.File
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

actual fun readFile(fileName: String): String {
    val inputStream = File(fileName).inputStream()
    val inputString = inputStream.bufferedReader().use { it.readText() }
    return inputString
}

actual fun Double.format(decimalNumber: Int): String =
        "%.${decimalNumber}f".format(this)

actual fun writeToFile(fileName: String, text: String) {
    File(fileName).printWriter().use { out ->
        out.println(text)
    }
}

actual fun assert(value: Boolean, lazyMessage: () -> Any) =
    kotlin.assert(value, lazyMessage)

// Create http(-s) request.
fun getHttpRequest(url: String, user: String?, password: String?): HttpURLConnection {
    val connection = URL(url).openConnection() as HttpURLConnection
    if (user != null && password != null) {
        val auth = Base64.getEncoder().encode((user + ":" + password).toByteArray()).toString(Charsets.UTF_8)
        connection.addRequestProperty("Authorization", "Basic $auth")
    }
    connection.setRequestProperty("Accept", "application/json")
    return connection
}

actual fun sendGetRequest(url: String, user: String?, password: String?, followLocation: Boolean) : String {
    val connection = getHttpRequest(url, user, password)
    connection.connect()
    val responseCode = connection.responseCode
    if (!followLocation) {
        connection.connect()
        return connection.inputStream.use { it.reader().use { reader -> reader.readText() } }
    }

    // Request with redirect.
    if (responseCode != HttpURLConnection.HTTP_MOVED_TEMP &&
            responseCode != HttpURLConnection.HTTP_MOVED_PERM &&
            responseCode != HttpURLConnection.HTTP_SEE_OTHER) {
        error("No opportunity to redirect, but flag for redirecting to location was provided!")
    }
    val newUrl = connection.getHeaderField("Location")
    val cookies = connection.getHeaderField("Set-Cookie")
    val redirect = getHttpRequest(newUrl, user, password)
    redirect.setRequestProperty("Cookie", cookies)
    redirect.connect()
    return redirect.inputStream.use { it.reader().use { reader -> reader.readText() } }
}