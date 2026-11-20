/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.toolchains

import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse

class ExternalToolchainProvisionerTest {
    @TempDir
    lateinit var temporaryDirectory: File

    private fun provisioner(
        offline: Boolean = false,
        downloaded: () -> Unit = {},
        unpack: (String, File, File) -> Unit = { _, _, directory ->
            directory.resolve("bin").mkdirs()
            directory.resolve("bin/tool").writeText("executable")
        },
    ) = ExternalToolchainProvisioner<String>(
        toolchainName = "TestTool",
        installationsDir = temporaryDirectory,
        installationName = { "tool-$it" },
        requiredFiles = { listOf("bin/tool") },
        archiveDownloader = ExternalToolchainArchiveDownloader(
            offline = offline,
            urlForRequest = { "https://example.com/$it.zip" },
            openStream = {
                downloaded()
                "archive".byteInputStream()
            },
        ),
        unpackArchive = unpack,
    )

    @Test
    fun `unpacker receives the request and archive and prepares the staged distribution`() {
        val installation = provisioner(unpack = { version, archive, destination ->
            assertEquals("133", version)
            assertEquals("archive", archive.readText())
            assertEquals(temporaryDirectory.resolve("tool-133.tmp/installation"), destination)
            destination.resolve("bin").mkdirs()
            destination.resolve("bin/tool").writeText("executable")
            destination.resolve("version.txt").writeText(version)
        }).provision("133")

        assertEquals("executable", installation.resolve("bin/tool").readText())
        assertEquals("133", installation.resolve("version.txt").readText())
        assertFalse(temporaryDirectory.resolve("tool-133.tmp").exists())
    }

    @Test
    fun `installs once per distribution and removes temporary files`() {
        var downloads = 0
        val provisioner = provisioner(downloaded = { downloads++ })

        val first = provisioner.provision("133")
        assertEquals(first, provisioner.provision(String(charArrayOf('1', '3', '3'))))
        assertEquals("executable", first.resolve("bin/tool").readText())
        assertEquals(1, downloads)
        assertFalse(temporaryDirectory.resolve("tool-133.tmp").exists())

        provisioner.provision("134")
        assertEquals(2, downloads)
    }

    @Test
    fun `offline service reuses an installation from a previous service`() {
        val installation = provisioner().provision("133")
        val offline = provisioner(offline = true, downloaded = { error("Must not download") })

        assertEquals(installation, offline.provision("133"))
        assertFailsWith<IOException> { offline.provision("134") }
        assertFalse(temporaryDirectory.resolve("tool-134.tmp").exists())
    }

    @Test
    fun `failed extraction is cleaned up and a later request retries`() {
        var attempts = 0
        val provisioner = provisioner(unpack = { _, _, directory ->
            directory.resolve("bin").mkdirs()
            if (++attempts == 1) throw IOException("Broken archive")
            directory.resolve("bin/tool").writeText("executable")
        })

        assertFailsWith<IOException> { provisioner.provision("133") }
        assertFalse(temporaryDirectory.resolve("tool-133").exists())
        assertFalse(temporaryDirectory.resolve("tool-133.tmp").exists())
        assertEquals("executable", provisioner.provision("133").resolve("bin/tool").readText())
        assertEquals(2, attempts)
    }

    @Test
    fun `does not publish an archive missing the executable`() {
        val provisioner = provisioner(unpack = { _, _, directory -> directory.mkdirs() })

        val failure = assertFailsWith<IllegalStateException> { provisioner.provision("133") }
        val archive = temporaryDirectory.resolve("tool-133.tmp/tool-133.archive")
        assertEquals("The TestTool toolchain archive '$archive' does not contain the required files: [bin/tool]", failure.message)
        assertFalse(temporaryDirectory.resolve("tool-133").exists())
        assertFalse(temporaryDirectory.resolve("tool-133.tmp").exists())
    }

    @Test
    fun `does not replace an incomplete installation`() {
        val installation = temporaryDirectory.resolve("tool-133")
        installation.mkdirs()
        installation.resolve("incomplete").writeText("old data")
        var downloads = 0

        val failure = assertFailsWith<IllegalStateException> { provisioner(downloaded = { downloads++ }).provision("133") }
        assertEquals(
            "The TestTool toolchain installation '$installation' is incomplete, it does not contain the required files: [bin/tool]. " +
                    "It is not replaced to keep its content; remove it to install the toolchain again.",
            failure.message,
        )
        assertEquals("old data", installation.resolve("incomplete").readText())
        assertEquals(0, downloads)
        assertFalse(temporaryDirectory.resolve("tool-133.tmp").exists())
    }

    @Test
    fun `independent provisioners share the installation lock`() {
        val downloads = AtomicInteger()
        val ready = CountDownLatch(2)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)
        try {
            val requests = List(2) {
                val provisioner = provisioner(downloaded = { downloads.incrementAndGet() })
                executor.submit<File> {
                    ready.countDown()
                    check(start.await(30, TimeUnit.SECONDS))
                    provisioner.provision("133")
                }
            }
            check(ready.await(30, TimeUnit.SECONDS))
            start.countDown()

            val installations = requests.map { it.get(30, TimeUnit.SECONDS) }
            assertEquals(installations[0], installations[1])
            assertEquals(1, downloads.get())
            assertEquals("executable", installations[0].resolve("bin/tool").readText())
        } finally {
            start.countDown()
            executor.shutdownNow()
        }
    }
}
