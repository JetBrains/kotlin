/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.konan.util

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLConnection
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.concurrent.*

typealias ProgressCallback = (url: String, currentBytes: Long, totalBytes: Long) -> Unit

class DependencyDownloader(
        var maxAttempts: Int = DEFAULT_MAX_ATTEMPTS,
        var attemptIntervalMs: Long = DEFAULT_ATTEMPT_INTERVAL_MS,
        customProgressCallback: ProgressCallback? = null
) {

    private val progressCallback = customProgressCallback ?: { url, currentBytes, totalBytes ->
        print("\rDownloading dependency: $url (${currentBytes.humanReadable}/${totalBytes.humanReadable}). ")
    }

    val executor = ExecutorCompletionService<Unit>(Executors.newSingleThreadExecutor(object : ThreadFactory {
        override fun newThread(r: Runnable?): Thread {
            val thread = Thread(r)
            thread.name = "konan-dependency-downloader"
            thread.isDaemon = true

            return thread
      }
    }))

    enum class ReplacingMode {
        /** Redownload the file and replace the existing one. */
        REPLACE,
        /** Throw FileAlreadyExistsException */
        THROW,
        /** Don't download the file and return the existing one*/
        RETURN_EXISTING
    }

    class HTTPResponseException(val url: URL, val responseCode: Int)
        : IOException("Server returned HTTP response code: $responseCode for URL: $url")

    class DownloadingProgress(@Volatile var currentBytes: Long) {
        fun update(readBytes: Int) { currentBytes += readBytes }
    }

    private fun HttpURLConnection.checkHTTPResponse(expected: Int, originalUrl: URL = url) {
        if (responseCode != expected) {
            throw HTTPResponseException(originalUrl, responseCode)
        }
    }

    private fun HttpURLConnection.checkHTTPResponse(originalUrl: URL, predicate: (Int) -> Boolean) {
        if (!predicate(responseCode)) {
            throw HTTPResponseException(originalUrl, responseCode)
        }
    }

    private fun doDownload(originalUrl: URL,
                           connection: URLConnection,
                           tmpFile: File,
                           currentBytes: Long,
                           totalBytes: Long,
                           append: Boolean) {
        val progress = DownloadingProgress(currentBytes)

        // TODO: Implement multi-thread downloading.
        executor.submit {
            connection.getInputStream().use { from ->
                FileOutputStream(tmpFile, append).use { to ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = from.read(buffer)
                    while (read != -1) {
                        if (Thread.interrupted()) {
                            throw InterruptedException()
                        }
                        to.write(buffer, 0, read)
                        progress.update(read)
                        read = from.read(buffer)
                    }
                    if (progress.currentBytes != totalBytes) {
                        throw EOFException("The stream closed before end of downloading.")
                    }
                }
            }
        }

        var result: Future<Unit>?
        do {
            progressCallback(originalUrl.toString(), progress.currentBytes, totalBytes)
            result = executor.poll(1, TimeUnit.SECONDS)
        } while(result == null)
        progressCallback(originalUrl.toString(), progress.currentBytes, totalBytes)

        try {
            result.get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    private fun resumeDownload(originalUrl: URL, originalConnection: HttpURLConnection, tmpFile: File) {
        originalConnection.connect()
        val totalBytes = originalConnection.contentLengthLong
        val currentBytes = tmpFile.length()
        if (currentBytes >= totalBytes || originalConnection.getHeaderField("Accept-Ranges") != "bytes") {
            // The temporary file is bigger then expected or the server doesn't support resuming downloading.
            // Download the file from scratch.
            doDownload(originalUrl, originalConnection, tmpFile, 0, totalBytes, false)
        } else {
            originalConnection.disconnect()
            val rangeConnection = originalUrl.openConnection() as HttpURLConnection
            rangeConnection.setRequestProperty("range", "bytes=$currentBytes-")
            rangeConnection.connect()
            rangeConnection.checkHTTPResponse(originalUrl) {
                it == HttpURLConnection.HTTP_PARTIAL || it == HttpURLConnection.HTTP_OK
            }
            doDownload(originalUrl, rangeConnection, tmpFile, currentBytes, totalBytes, true)
        }
    }

    /** Performs an attempt to download a specified file into the specified location */
    private fun tryDownload(url: URL, tmpFile: File) {
        val connection = url.openConnection()

        (connection as? HttpURLConnection)?.checkHTTPResponse(HttpURLConnection.HTTP_OK, url)

        if (connection is HttpURLConnection && tmpFile.exists()) {
            resumeDownload(url, connection, tmpFile)
        } else {
            connection.connect()
            val totalBytes = connection.contentLengthLong
            doDownload(url, connection, tmpFile, 0, totalBytes, false)
        }
    }

    /** Downloads a file from [source] url to [destination]. Returns [destination]. */
    fun download(source: URL,
                 destination: File,
                 replace: ReplacingMode = ReplacingMode.RETURN_EXISTING): File {

        if (destination.exists()) {
            when (replace) {
                ReplacingMode.RETURN_EXISTING -> return destination
                ReplacingMode.THROW -> throw FileAlreadyExistsException(destination)
                ReplacingMode.REPLACE -> Unit // Just continue with downloading.
            }
        }
        val tmpFile = File("${destination.canonicalPath}.$TMP_SUFFIX")

        check(!tmpFile.isDirectory) {
            "A temporary file is a directory: ${tmpFile.canonicalPath}. Remove it and try again."
        }
        check(!destination.isDirectory) {
            "The destination file is a directory: ${destination.canonicalPath}. Remove it and try again."
        }

        var attempt = 1
        var waitTime = 0L
        val handleException = { e: Exception ->
            if (attempt >= maxAttempts) {
                throw e
            }
            attempt++
            waitTime += attemptIntervalMs
            println("Cannot download a dependency: $e\n" +
                    "Waiting ${waitTime.toDouble() / 1000} sec and trying again (attempt: $attempt/$maxAttempts).")
            // TODO: Wait better
            Thread.sleep(waitTime)
        }
        while (true) {
            try {
                tryDownload(source, tmpFile)
                break
            } catch (e: HTTPResponseException) {
                if (e.responseCode >= 500) {
                    // Retry server errors.
                    handleException(e)
                } else {
                    // Do not retry client errors.
                    throw e
                }
            } catch (e: IOException) {
                handleException(e)
            }
        }

        Files.move(tmpFile.toPath(), destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
        println("Done.")
        return destination
    }

    private val Long.humanReadable: String
        get() {
            if (this < 0) {
                return "-"
            }
            if (this < 1024) {
                return "$this bytes"
            }
            val exp = (Math.log(this.toDouble()) / Math.log(1024.0)).toInt()
            val prefix = "kMGTPE"[exp-1]
            return "%.1f %siB".format(this / Math.pow(1024.0, exp.toDouble()), prefix)
        }

    companion object {
        const val DEFAULT_MAX_ATTEMPTS = 10
        const val DEFAULT_ATTEMPT_INTERVAL_MS = 3000L

        const val TMP_SUFFIX = "part"
    }
}
