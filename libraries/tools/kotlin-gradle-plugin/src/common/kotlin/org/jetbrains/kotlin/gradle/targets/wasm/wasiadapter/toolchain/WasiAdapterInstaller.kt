/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.wasm.wasiadapter.toolchain

import org.gradle.api.logging.Logger
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterFlavor
import org.jetbrains.kotlin.gradle.dsl.toolchain.wasiadapter.WasiAdapterVersion
import org.jetbrains.kotlin.gradle.targets.native.internal.KotlinInterprocessDirectoryLock
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@OptIn(ExperimentalWasmDsl::class)
internal class WasiAdapterInstaller(
    private val logger: Logger,
) {
    /**
     * Returns the WASI preview1 adapter file for [version] and [flavor], installing (downloading) it if needed.
     *
     * @param installationsDir the directory containing all WASI adapter installations.
     * @param version the requested Wasmtime version.
     * @param flavor the requested WASI adapter flavor.
     * @param downloadBaseUrl the base URL of the Wasmtime distributions.
     * @param offline whether downloading is forbidden.
     */
    fun installAdapter(
        installationsDir: File,
        version: WasiAdapterVersion,
        flavor: WasiAdapterFlavor,
        downloadBaseUrl: String,
        offline: Boolean,
    ): File {
        val adapterDir = wasiAdapterDirectory(installationsDir, version)
        val fileName = wasiAdapterFileName(flavor)
        val adapterPath = adapterDir.resolve(fileName)

        if (isCompleteAdapterInstallation(adapterPath)) return adapterPath

        if (offline) {
            throw IOException(
                "WASI preview1 adapter '$fileName' for Wasmtime ${version.normalized} is not installed in '$adapterDir', " +
                    "and the build is running in offline mode, so it cannot be downloaded.\n" +
                    "Either run the build without '--offline', or configure an explicit WASI adapter file in the WASI adapter toolchain."
            )
        }

        val lockDir = adapterDir.resolve(".$fileName.lock")
        KotlinInterprocessDirectoryLock(lockDir, logInfo = { logger.info(it) }).withLock {
            if (isCompleteAdapterInstallation(adapterPath)) return@withLock

            val tempFile = File("${adapterPath.path}$TEMP_DIR_SUFFIX")
            tempFile.delete()

            try {
                val versionUrl = "${downloadBaseUrl.trimEnd('/')}/v${version.normalized}"
                val adapterUrl = "$versionUrl/$fileName"

                logger.lifecycle("Downloading WASI preview1 adapter $flavor for Wasmtime $version from $adapterUrl")
                downloadFile(adapterUrl, tempFile)

                adapterDir.mkdirs()
                try {
                    Files.move(tempFile.toPath(), adapterPath.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                } catch (e: Exception) {
                    throw IllegalStateException("WASI preview1 adapter could not be installed at '$adapterPath'", e)
                }
            } finally {
                tempFile.delete()
            }

            logger.info("Installed WASI preview1 adapter $flavor for Wasmtime $version into '$adapterPath'")
        }

        return adapterPath
    }

    private fun isCompleteAdapterInstallation(adapterPath: File): Boolean =
        adapterPath.isFile && adapterPath.length() > 0

    private fun downloadFile(url: String, target: File) {
        target.parentFile?.mkdirs()

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

    private fun openConnection(url: String): InputStream {
        val connection = URL(url).openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        if (connection is HttpURLConnection) {
            connection.instanceFollowRedirects = true
            if (connection.responseCode !in 200..299) {
                val message = "$url returned HTTP ${connection.responseCode} ${connection.responseMessage}"
                connection.disconnect()
                throw IOException(message)
            }
        }
        return connection.inputStream
    }

    private companion object {
        private const val TEMP_DIR_SUFFIX = ".tmp"
        private const val DOWNLOAD_ATTEMPTS = 3
        private const val CONNECT_TIMEOUT_MS = 30_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
