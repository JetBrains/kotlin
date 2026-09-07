/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.logging.Logging
import org.gradle.internal.extensions.core.serviceOf
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import java.io.File
import java.io.IOException
import kotlin.io.path.createTempDirectory
import kotlin.io.path.isSymbolicLink
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Tests [NodeJsDistributionInstaller] against a [FakeNodeJsDistributionServer], so that the tests are
 * hermetic and do not download anything from the network.
 *
 * Unless a test is about the platform-specific layout, the `win` platform is used on purpose: its
 * distributions are ZIP archives, and their layout does not need any symlinks.
 */
class NodeJsDistributionInstallerTest {

    private val project = ProjectBuilder.builder().build()

    private val installer = NodeJsDistributionInstaller(
        fs = project.serviceOf<FileSystemOperations>(),
        archiveOperations = project.serviceOf<ArchiveOperations>(),
        logger = Logging.getLogger(NodeJsDistributionInstallerTest::class.java),
    )

    private val installationsDir: File = createTempDirectory("node-toolchain-test").toFile()

    private val startedServers = mutableListOf<FakeNodeJsDistributionServer>()

    private lateinit var distributionServer: FakeNodeJsDistributionServer

    @BeforeTest
    fun setUp() {
        distributionServer = startServer(WINDOWS_PLATFORM)
    }

    @AfterTest
    fun tearDown() {
        startedServers.forEach { it.stop() }
        installationsDir.deleteRecursively()
    }

    @Test
    fun `when a distribution is requested, expect it is downloaded and unpacked`() {
        val installationDir = install()

        assertEquals(installationsDir.resolve("node-v24.16.0-win-x64"), installationDir)
        val executable = nodeJsExecutableFile(installationDir, WINDOWS_PLATFORM)
        assertTrue(executable.isFile, "Expected the Node.js executable at '$executable'")
        assertEquals(FakeNodeJsDistributionServer.FAKE_NODE_CONTENT, executable.readText())
    }

    @Test
    fun `when a distribution is already installed, expect it is not downloaded again`() {
        install()
        assertEquals(1, distributionServer.archiveRequests.get())

        install()
        assertEquals(
            1,
            distributionServer.archiveRequests.get(),
            "Expected the already installed distribution to be reused",
        )
    }

    @Test
    fun `when the downloaded archive does not match the published checksum, expect a failure`() {
        distributionServer.corruptChecksums = true

        val failure = assertFailsWithIOException { install() }

        assertContains(failure.message.orEmpty(), "does not match the checksum published at")
        assertFalse(
            installationsDir.resolve("node-v24.16.0-win-x64").exists(),
            "Expected no installation to be left behind after a failed verification",
        )
    }

    @Test
    fun `when the archive is not listed in the published checksums, expect a failure`() {
        distributionServer.listArchiveInChecksums = false

        val failure = assertFailsWithIOException { install() }

        assertContains(failure.message.orEmpty(), "is not listed in the checksums published at")
    }

    @Test
    fun `when the published checksums are not available, expect a failure`() {
        distributionServer.publishChecksums = false

        val failure = assertFailsWithIOException { install() }

        assertContains(failure.message.orEmpty(), "HTTP 404")
    }

    @Test
    fun `when verification is disabled, expect the download is not verified`() {
        distributionServer.publishChecksums = false

        val installationDir = install(verifyDownload = false)

        assertTrue(nodeJsExecutableFile(installationDir, WINDOWS_PLATFORM).isFile)
    }

    @Test
    fun `when offline and the distribution is missing, expect a failure without downloading`() {
        val failure = assertFailsWithIOException { install(offline = true) }

        assertContains(failure.message.orEmpty(), "the build is running in offline mode")
        assertEquals(0, distributionServer.archiveRequests.get(), "Expected nothing to be downloaded")
    }

    @Test
    fun `when offline and the distribution is installed, expect it is reused`() {
        install()

        val installationDir = install(offline = true)

        assertTrue(nodeJsExecutableFile(installationDir, WINDOWS_PLATFORM).isFile)
        assertEquals(1, distributionServer.archiveRequests.get())
    }

    @Test
    fun `when an incomplete installation is present, expect it is replaced`() {
        val target = installationsDir.resolve("node-v24.16.0-win-x64")
        target.mkdirs()
        target.resolve("leftover.txt").writeText("incomplete installation")

        val installationDir = install()

        assertTrue(nodeJsExecutableFile(installationDir, WINDOWS_PLATFORM).isFile)
        assertFalse(
            installationDir.resolve("leftover.txt").exists(),
            "Expected the incomplete installation to be removed",
        )
    }

    @Test
    fun `when a non-Windows distribution is installed, expect the executable bit and the npm symlinks are restored`() {
        Assumptions.assumeFalse(HostManager.hostIsMingw, "Symlinks are not always available on Windows")

        val server = startServer(UNIX_PLATFORM)

        val installationDir = install(platform = UNIX_PLATFORM, server = server)

        val executable = nodeJsExecutableFile(installationDir, UNIX_PLATFORM)
        assertTrue(executable.isFile, "Expected the Node.js executable at '$executable'")
        assertTrue(executable.canExecute(), "Expected '$executable' to be executable")

        for (script in listOf("npm", "npx")) {
            val scriptFile = installationDir.resolve("bin/$script")
            assertTrue(
                scriptFile.toPath().isSymbolicLink(),
                "Expected '$scriptFile' to be a symlink to the npm script",
            )
            assertEquals(
                installationDir.resolve("lib/node_modules/npm/bin/$script-cli.js").canonicalFile,
                scriptFile.canonicalFile,
            )
        }
    }

    private fun startServer(platform: BuildPlatform): FakeNodeJsDistributionServer =
        FakeNodeJsDistributionServer(VERSION, platform)
            .also {
                it.start()
                startedServers += it
            }

    private fun install(
        platform: BuildPlatform = WINDOWS_PLATFORM,
        server: FakeNodeJsDistributionServer = distributionServer,
        verifyDownload: Boolean = true,
        offline: Boolean = false,
    ): File = installer.install(
        installationsDir = installationsDir,
        version = VERSION,
        platform = platform,
        downloadBaseUrl = server.baseUrl,
        verifyDownload = verifyDownload,
        offline = offline,
    )

    private inline fun assertFailsWithIOException(block: () -> Unit): IOException {
        try {
            block()
        } catch (e: IOException) {
            return e
        } catch (e: Throwable) {
            fail("Expected an IOException, but got $e")
        }
        fail("Expected an IOException, but nothing was thrown")
    }

    private companion object {
        private val VERSION = NodeJsVersion("24.16.0")
        private val WINDOWS_PLATFORM = BuildPlatform("win", "x64")
        private val UNIX_PLATFORM = BuildPlatform("linux", "x64")
    }
}
