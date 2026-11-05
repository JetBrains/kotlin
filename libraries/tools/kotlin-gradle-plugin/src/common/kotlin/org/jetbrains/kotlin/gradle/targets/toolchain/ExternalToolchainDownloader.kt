/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinToolchainDsl::class)

package org.jetbrains.kotlin.gradle.targets.toolchain

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExperimentalKotlinToolchainDsl
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Knows how to download the archive of a toolchain distribution from an external resource.
 */
internal fun interface ExternalToolchainDownloader<in T : ExternalToolchainForKotlin> {

    /**
     * Downloads the archive of [toolchain] into [targetDir].
     *
     * @return the downloaded archive.
     * @throws IOException when the archive cannot be downloaded.
     */
    fun download(toolchain: T, targetDir: File, logger: Logger): File
}

/**
 * Downloads the archive over HTTP from the URL returned by [urlForRequest], retrying transient failures.
 */
internal class DefaultExternalToolchainDownloader<in T : ExternalToolchainForKotlin>(
    private val urlForRequest: (T) -> String,
) : ExternalToolchainDownloader<T> {

    override fun download(toolchain: T, targetDir: File, logger: Logger): File {
        val url = urlForRequest(toolchain)
        val archive = targetDir.resolve(url.substringAfterLast('/'))

        logger.lifecycle("Downloading ${toolchain.toolName} ${toolchain.version.version} for ${toolchain.platform} from $url")
        download(url, archive, logger)
        return archive
    }

    private fun download(url: String, target: File, logger: Logger) {
        target.parentFile.mkdirs()

        var lastFailure: IOException? = null
        repeat(DOWNLOAD_ATTEMPTS) { attempt ->
            try {
                openConnection(url).use { input ->
                    target.outputStream().use(input::copyTo)
                }
                return
            } catch (e: IOException) {
                lastFailure = e
                target.delete()
                logger.info("Failed to download $url (attempt ${attempt + 1} of $DOWNLOAD_ATTEMPTS): ${e.message}")
            }
        }
        throw IOException("Cannot download $url", lastFailure)
    }

    private fun openConnection(url: String): InputStream =
        (URL(url).openConnection() as HttpURLConnection).run {
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            if (responseCode !in 200..299) {
                val message = "$url returned HTTP $responseCode $responseMessage"
                disconnect()
                throw IOException(message)
            }
            inputStream
        }

    private companion object {
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
