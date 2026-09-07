/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicInteger
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Serves a single fake Node.js distribution the very same way the official distribution does,
 * so that the toolchain tests are hermetic and never download anything from the network.
 *
 * The layout of the served archive follows the official distributions: Windows distributions are
 * ZIP archives with `node.exe` at their root, all the others are gzipped TAR archives with the
 * executables in `bin` and the npm scripts in `lib/node_modules`.
 */
internal class FakeNodeJsDistributionServer(
    version: NodeJsVersion,
    private val platform: BuildPlatform,
) {
    /** Whether `SHASUMS256.txt` is available at all. */
    var publishChecksums: Boolean = true

    /** Whether `SHASUMS256.txt` contains an entry for the served archive. */
    var listArchiveInChecksums: Boolean = true

    /** Whether the published checksum of the served archive is a wrong one. */
    var corruptChecksums: Boolean = false

    /** How many times the distribution archive has been requested. */
    val archiveRequests = AtomicInteger()

    private val distributionName = nodeJsDistributionName(version, platform)
    private val archiveName = "$distributionName.${nodeJsArchiveExtension(platform)}"
    private val versionPath = "/v${version.normalized}"

    private val archive: ByteArray =
        if (platform.isWindows) zipArchive(distributionName) else tarGzArchive(distributionName)

    private val server: HttpServer = HttpServer.create(InetSocketAddress(LOOPBACK, 0), 0)

    val baseUrl: String get() = "http://$LOOPBACK:${server.address.port}"

    fun start() {
        server.createContext("$versionPath/$archiveName") { exchange ->
            archiveRequests.incrementAndGet()
            exchange.respond(archive)
        }
        server.createContext("$versionPath/$CHECKSUMS_FILE_NAME") { exchange ->
            if (!publishChecksums) {
                exchange.sendResponseHeaders(404, -1)
                exchange.close()
                return@createContext
            }
            val checksum = if (corruptChecksums) "0".repeat(64) else archive.sha256()
            val name = if (listArchiveInChecksums) archiveName else "some-other-distribution.zip"
            exchange.respond("$checksum  $name\n".toByteArray())
        }
        server.start()
    }

    fun stop() = server.stop(0)

    private fun HttpExchange.respond(body: ByteArray) {
        sendResponseHeaders(200, body.size.toLong())
        responseBody.use { it.write(body) }
    }

    /**
     * The content of the entries of the served distribution, keyed by their path inside the archive.
     */
    private fun distributionEntries(distributionName: String): Map<String, String> =
        if (platform.isWindows) {
            mapOf("$distributionName/$WINDOWS_NODE_EXECUTABLE_NAME" to FAKE_NODE_CONTENT)
        } else {
            mapOf(
                "$distributionName/bin/node" to FAKE_NODE_CONTENT,
                // The official archives contain symlinks here, but Gradle unpacks them as regular files.
                "$distributionName/bin/npm" to "fake npm",
                "$distributionName/bin/npx" to "fake npx",
                "$distributionName/lib/node_modules/npm/bin/npm-cli.js" to "fake npm-cli",
                "$distributionName/lib/node_modules/npm/bin/npx-cli.js" to "fake npx-cli",
            )
        }

    private fun zipArchive(distributionName: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        ZipOutputStream(bytes).use { zip ->
            for ((path, content) in distributionEntries(distributionName)) {
                zip.putNextEntry(ZipEntry(path))
                zip.write(content.toByteArray())
                zip.closeEntry()
            }
        }
        return bytes.toByteArray()
    }

    private fun tarGzArchive(distributionName: String): ByteArray {
        val bytes = ByteArrayOutputStream()
        GZIPOutputStream(bytes).use { gzip ->
            for ((path, content) in distributionEntries(distributionName)) {
                gzip.writeTarEntry(path, content.toByteArray())
            }
            // A TAR archive ends with two empty blocks.
            gzip.write(ByteArray(2 * TAR_BLOCK_SIZE))
        }
        return bytes.toByteArray()
    }

    /**
     * Writes a single regular file entry in the `ustar` format, which is the format of the official
     * Node.js distribution archives.
     */
    private fun OutputStream.writeTarEntry(path: String, content: ByteArray) {
        val header = ByteArray(TAR_BLOCK_SIZE)
        header.putString(path, offset = 0, length = 100)
        header.putString("0000755\u0000", offset = 100, length = 8) // mode
        header.putString("0000000\u0000", offset = 108, length = 8) // uid
        header.putString("0000000\u0000", offset = 116, length = 8) // gid
        header.putString(content.size.toOctal(11) + "\u0000", offset = 124, length = 12) // size
        header.putString(0.toOctal(11) + "\u0000", offset = 136, length = 12) // mtime
        header.putString(" ".repeat(8), offset = 148, length = 8) // checksum placeholder
        header.putString("0", offset = 156, length = 1) // a regular file
        header.putString("ustar\u000000", offset = 257, length = 8) // magic and version

        val checksum = header.sumOf { it.toInt() and 0xff }
        header.putString(checksum.toOctal(6) + "\u0000 ", offset = 148, length = 8)

        write(header)
        write(content)
        val padding = (TAR_BLOCK_SIZE - content.size % TAR_BLOCK_SIZE) % TAR_BLOCK_SIZE
        write(ByteArray(padding))
    }

    private fun ByteArray.putString(value: String, offset: Int, length: Int) {
        val encoded = value.toByteArray()
        require(encoded.size <= length) { "'$value' does not fit into $length bytes" }
        encoded.copyInto(this, offset)
    }

    private fun Int.toOctal(length: Int): String = toString(8).padStart(length, '0')

    private fun ByteArray.sha256(): String =
        MessageDigest.getInstance("SHA-256").digest(this).joinToString("") { "%02x".format(it) }

    companion object {
        /** The content of the fake `node` executable the served distribution contains. */
        const val FAKE_NODE_CONTENT = "fake node"

        private const val LOOPBACK = "127.0.0.1"

        private const val CHECKSUMS_FILE_NAME = "SHASUMS256.txt"

        private const val WINDOWS_NODE_EXECUTABLE_NAME = "node.exe"

        private const val TAR_BLOCK_SIZE = 512
    }
}
