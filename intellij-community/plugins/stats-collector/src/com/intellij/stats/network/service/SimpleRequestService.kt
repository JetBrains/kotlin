/*
 * Copyright 2000-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.stats.network.service

import com.google.common.net.HttpHeaders
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.text.StringUtil
import com.intellij.util.io.HttpRequests
import org.apache.commons.codec.binary.Base64OutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URLConnection
import java.util.zip.GZIPOutputStream

class SimpleRequestService: RequestService() {
    private companion object {
        val LOG = logger<SimpleRequestService>()
    }

    override fun postZipped(url: String, file: File): ResponseData? {
        return try {
            val zippedArray = getZippedContent(file)
            return HttpRequests.post(url, null).tuner {
                it.setRequestProperty(HttpHeaders.CONTENT_ENCODING, "gzip")
            }.connect { request ->
                request.write(zippedArray)
                return@connect request.connection.asResponseData(zippedArray.size)
            }
        }
        catch (e: HttpRequests.HttpStatusException) {
            ResponseData(e.statusCode, StringUtil.notNullize(e.message))
        }
        catch (e: IOException) {
            LOG.debug(e)
            null
        }
    }

    private fun getZippedContent(file: File): ByteArray {
        val fileText = file.readText()
        return GzipBase64Compressor.compress(fileText)
    }

    override fun get(url: String): ResponseData? {
        return try {
            val requestBuilder = HttpRequests.request(url)
            return requestBuilder.connect { request ->
                val responseCode = request.getResponseCode()
                val responseText = request.readString()
                ResponseData(responseCode, responseText)
            }
        } catch (e: IOException) {
            LOG.debug(e)
            null
        }
    }

    private fun URLConnection.asResponseData(sentDataSize: Int?): ResponseData? {
        if (this is HttpURLConnection) {
            return ResponseData(this.responseCode, StringUtil.notNullize(this.responseMessage, ""), sentDataSize)
        }

        LOG.error("Could not get code and message from http response")
        return null
    }

    private fun HttpRequests.Request.getResponseCode(): Int {
        val connection = this.connection
        if (connection is HttpURLConnection) {
            return connection.responseCode
        }

        LOG.error("Could not get code from http response")
        return -1
    }
}


data class ResponseData(val code: Int, val text: String = "", val sentDataSize: Int? = null) {
    fun isOK(): Boolean = code in 200..299
}


object GzipBase64Compressor {
    fun compress(text: String): ByteArray {
        val outputStream = ByteArrayOutputStream()
        val base64Stream = GZIPOutputStream(Base64OutputStream(outputStream))
        base64Stream.write(text.toByteArray())
        base64Stream.close()
        return outputStream.toByteArray()
    }
}