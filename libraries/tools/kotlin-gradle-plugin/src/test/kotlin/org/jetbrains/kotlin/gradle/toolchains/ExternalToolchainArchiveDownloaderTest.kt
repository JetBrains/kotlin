/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.toolchains

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExternalToolchainArchiveDownloaderTest {
    @TempDir
    lateinit var temporaryDirectory: File

    @Test
    fun `maps the request and closes the downloaded stream`() {
        var closed = false
        val downloader = ExternalToolchainArchiveDownloader<Int>(
            offline = false,
            urlForRequest = { "https://example.com/$it.tar.gz" },
            openStream = { url ->
                assertEquals("https://example.com/133.tar.gz", url)
                object : ByteArrayInputStream("archive".toByteArray()) {
                    override fun close() {
                        closed = true
                        super.close()
                    }
                }
            },
        )
        val archive = temporaryDirectory.resolve("download/archive.tar.gz")

        downloader.download(133, archive)

        assertEquals("archive", archive.readText())
        assertTrue(closed)
    }

    @Test
    fun `offline mode fails before mapping or opening the URL`() {
        val downloader = ExternalToolchainArchiveDownloader<String>(
            offline = true,
            urlForRequest = { error("Must not map a request in offline mode") },
            openStream = { error("Must not open a connection in offline mode") },
        )
        val archive = temporaryDirectory.resolve("download/archive.tar.gz")

        assertFailsWith<IOException> { downloader.checkIfCanBeDownloaded() }
        assertFailsWith<IOException> { downloader.download("133", archive) }
        assertFalse(archive.parentFile.exists())
    }

    @Test
    fun `retries a failed transfer without retaining partial contents`() {
        var attempts = 0
        var failedStreamClosed = false
        val downloader = ExternalToolchainArchiveDownloader<String>(
            offline = false,
            urlForRequest = { it },
            openStream = {
                attempts++
                if (attempts == 1) object : InputStream() {
                    private var read = false

                    override fun read(): Int {
                        if (read) throw IOException("Interrupted transfer")
                        read = true
                        return 'x'.code
                    }

                    override fun close() {
                        failedStreamClosed = true
                    }
                } else ByteArrayInputStream("complete".toByteArray())
            },
        )
        val archive = temporaryDirectory.resolve("archive.tar.gz")

        downloader.download("https://example.com/archive.tar.gz", archive)

        assertEquals(2, attempts)
        assertEquals("complete", archive.readText())
        assertTrue(failedStreamClosed)
    }

    @Test
    fun `exhausted retries remove the archive and retain the cause`() {
        var attempts = 0
        val failure = IOException("Connection unavailable")
        val downloader = ExternalToolchainArchiveDownloader<String>(
            offline = false,
            downloadAttempts = 2,
            urlForRequest = { it },
            openStream = {
                attempts++
                throw failure
            },
        )
        val archive = temporaryDirectory.resolve("archive.tar.gz")
        archive.writeText("incomplete")

        val actual = assertFailsWith<IOException> { downloader.download("https://example.com/archive.tar.gz", archive) }

        assertEquals(2, attempts)
        assertSame(failure, actual.cause)
        assertFalse(archive.exists())
    }
}
