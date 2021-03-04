/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.example.weather_func

import org.example.weather_func.Event
import platform.posix.size_t

import curl.curl_easy_setopt
import curl.CURLOPT_URL
import curl.CURLOPT_HEADERFUNCTION
import curl.CURLOPT_HEADERDATA
import curl.CURLOPT_WRITEFUNCTION
import curl.CURLOPT_WRITEDATA
import curl.curl_easy_cleanup
import curl.curl_easy_init
import curl.CURLE_OK
import curl.curl_easy_strerror
import curl.curl_easy_perform

import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.readBytes

/**
 * Provides basic HTTP client functionality through the libCurl library.
*/
internal class CUrl(val url: String) {
    private val stableRef = StableRef.create(this)
    private val curlObj = curl_easy_init()
    val header = Event<String>()
    val body = Event<String>()

    init {
        val header = staticCFunction(::headerCallback)
        val writeData = staticCFunction(::writeCallback)

        // Setup Curl.
        curl_easy_setopt(curlObj, CURLOPT_URL, url)
        curl_easy_setopt(curlObj, CURLOPT_HEADERFUNCTION, header)
        curl_easy_setopt(curlObj, CURLOPT_HEADERDATA, stableRef.asCPointer())
        curl_easy_setopt(curlObj, CURLOPT_WRITEFUNCTION, writeData)
        curl_easy_setopt(curlObj, CURLOPT_WRITEDATA, stableRef.asCPointer())
    }

    fun fetch() {
        // Have Curl do a HTTP/HTTPS request and store the response (status).
        val response = curl_easy_perform(curlObj)
        val maxChars = 50
        // utfCharSize in bytes.
        val utfCharSize = 4
        // length in bytes.
        val length = utfCharSize * maxChars
        // Print the error message if the Curl status code isn't OK (CURLE_OK).
        if (response != CURLE_OK) println("Curl HTTP/S request failed: ${curl_easy_strerror(response)?.toKString(length)}")
    }

    fun close() {
        curl_easy_cleanup(curlObj)
        stableRef.dispose()
    }
}

fun headerCallback(buffer: CPointer<ByteVar>?, size: size_t, totalItems: size_t, userData: COpaquePointer?): size_t {
    var responseSize = 0L
    if (buffer != null && userData != null) {
        val header = buffer.toKString((size * totalItems).toInt()).trim()
        val curlRef = userData.asStableRef<CUrl>().get()

        curlRef.header(header)
        responseSize = size * totalItems
    }
    return responseSize
}

fun writeCallback(buffer: CPointer<ByteVar>?, size: size_t, totalItems: size_t, userData: COpaquePointer?): size_t {
    var responseSize = 0L
    if (buffer != null && userData != null) {
        val data = buffer.toKString((size * totalItems).toInt()).trim()
        val curlRef = userData.asStableRef<CUrl>().get()

        curlRef.body(data)
        responseSize = size * totalItems
    }
    return responseSize
}

private fun CPointer<ByteVar>.toKString(length: Int) = readBytes(length).decodeToString()
