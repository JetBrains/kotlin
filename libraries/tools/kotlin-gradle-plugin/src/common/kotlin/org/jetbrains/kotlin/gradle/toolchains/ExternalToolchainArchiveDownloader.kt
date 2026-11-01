/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.toolchains

import org.gradle.api.logging.Logging
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Downloads an archive for a request, leaving installation layout and extraction to the caller. */
internal class ExternalToolchainArchiveDownloader<in R>(
    /** Forbids downloads when offline. */
    private val offline: Boolean,

    /** Returns the URL for a request, throwing if the request is invalid. */
    private val urlForRequest: (R) -> String,

    /** Opens the stream used to download an archive. */
    private val openStream: (url: String) -> InputStream = ::openExternalToolchainArchiveStream,

    private val downloadAttempts: Int = 3,
) {
    init {
        require(downloadAttempts > 0) { "Download attempts must be positive" }
    }

    fun checkIfCanBeDownloaded() {
        if (offline) {
            throw IOException(
                "The requested toolchain is not installed and cannot be downloaded in offline mode. " +
                        "Run the build without '--offline' or configure a pre-installed toolchain."
            )
        }
    }

    fun download(request: R, destination: File) {
        checkIfCanBeDownloaded()
        val url = urlForRequest(request)
        destination.parentFile.mkdirs()
        logger.info("Downloading toolchain from $url")

        var lastFailure: IOException? = null
        repeat(downloadAttempts) { attempt ->
            try {
                openStream(url).use { input ->
                    destination.outputStream().use(input::copyTo)
                }
                return
            } catch (failure: IOException) {
                lastFailure = failure
                destination.delete()
                logger.info("Failed to download $url (attempt ${attempt + 1} of $downloadAttempts): ${failure.message}")
            }
        }
        throw IOException("Cannot download $url", lastFailure)
    }

    private companion object {
        val logger = Logging.getLogger(ExternalToolchainArchiveDownloader::class.java)
    }
}

private fun openExternalToolchainArchiveStream(url: String): InputStream {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 30_000
        connection.readTimeout = 60_000
        connection.instanceFollowRedirects = true
        if (connection.responseCode !in 200..299) {
            throw IOException("$url returned HTTP ${connection.responseCode} ${connection.responseMessage}")
        }
        return object : FilterInputStream(connection.inputStream) {
            override fun close() {
                try {
                    super.close()
                } finally {
                    connection.disconnect()
                }
            }
        }
    } catch (failure: Exception) {
        connection.disconnect()
        throw failure
    }
}
