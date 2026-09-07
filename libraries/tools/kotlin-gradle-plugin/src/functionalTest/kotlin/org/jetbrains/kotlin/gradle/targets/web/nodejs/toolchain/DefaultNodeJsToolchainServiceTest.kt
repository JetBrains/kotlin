/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.util.assertLogContains
import org.jetbrains.kotlin.gradle.util.withGradleLogCapture
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertTrue

/**
 * Tests [DefaultNodeJsToolchainService] against a [FakeNodeJsDistributionServer], so that the tests are
 * hermetic and do not download anything from the network.
 *
 * The `win` platform is used on purpose: its distributions are ZIP archives, and their layout does not
 * need any symlinks, so the tests behave the same way on all hosts.
 */
class DefaultNodeJsToolchainServiceTest {

    private val project = ProjectBuilder.builder().build()

    private val installationsDir: File = createTempDirectory("node-toolchain-service-test").toFile()

    private val startedServers = mutableListOf<FakeNodeJsDistributionServer>()

    private var registeredServices = 0

    @AfterTest
    fun tearDown() {
        startedServers.forEach { it.stop() }
        installationsDir.deleteRecursively()
    }

    @Test
    fun `when a version is requested, expect it is provisioned only once the provider is queried`() {
        val server = startServer(VERSION)
        val service = createService(server)

        val nodeJs = service.request { version(VERSION.version) }
        assertEquals(0, server.archiveRequests.get(), "Expected no provisioning at configuration time")

        val provisioned = nodeJs.get()

        assertEquals(1, server.archiveRequests.get())
        assertEquals(VERSION, provisioned.version.get())
        assertEquals(WINDOWS_PLATFORM, provisioned.platform.get())
        val expectedExecutable = nodeJsExecutableFile(
            installationsDir.resolve(nodeJsDistributionName(VERSION, WINDOWS_PLATFORM)),
            WINDOWS_PLATFORM,
        )
        assertEquals(expectedExecutable.absolutePath, provisioned.executable.get())
        assertTrue(expectedExecutable.isFile, "Expected the Node.js executable at '$expectedExecutable'")
    }

    @Test
    fun `when the same version is requested twice, expect it is provisioned once`() {
        val server = startServer(VERSION)
        val service = createService(server)

        service.request { version(VERSION.version) }.get()
        // A memoized installation is not expected to be provisioned again, even if it is gone from the disk.
        installationsDir.deleteRecursively()

        service.request { version(VERSION.version) }.get()

        assertEquals(1, server.archiveRequests.get(), "Expected the distribution to be provisioned once")
    }

    @Test
    fun `when a custom download base url is configured, expect the download is not verified`() {
        val server = startServer(VERSION).also { it.corruptChecksums = true }
        val service = createService(server)

        val provisioned = service.request { version(VERSION.version) }.get()

        assertTrue(File(provisioned.executable.get()).isFile)
    }

    @Test
    fun `when an unsupported version is requested, expect a warning and a successful provisioning`() {
        val server = startServer(UNSUPPORTED_VERSION)
        val service = createService(server)

        lateinit var provisioned: NodeJsExecutable
        val logs = withGradleLogCapture {
            provisioned = service.request { version(UNSUPPORTED_VERSION.version) }.get()
        }

        logs.assertLogContains("Node.js $UNSUPPORTED_VERSION is not supported by the Kotlin Gradle Plugin")
        assertTrue(File(provisioned.executable.get()).isFile)
    }

    @Test
    fun `when no version is requested, expect a failure`() {
        val service = createService(startServer(VERSION))

        val failure = assertFails { service.request().get() }

        assertContains(failure.message.orEmpty(), "A Node.js version must be requested")
    }

    private fun startServer(version: NodeJsVersion): FakeNodeJsDistributionServer =
        FakeNodeJsDistributionServer(version, WINDOWS_PLATFORM)
            .also {
                it.start()
                startedServers += it
            }

    private fun createService(
        server: FakeNodeJsDistributionServer,
        offline: Boolean = false,
    ): DefaultNodeJsToolchainService = project.gradle.sharedServices
        .registerIfAbsent(
            "nodeJsToolchain${registeredServices++}",
            DefaultNodeJsToolchainService::class.java,
        ) { spec ->
            spec.parameters.defaultPlatform.set(WINDOWS_PLATFORM)
            spec.parameters.installationDir.set(installationsDir)
            spec.parameters.downloadBaseUrl.set(server.baseUrl)
            spec.parameters.offline.set(offline)
        }
        .get()

    private companion object {
        private val VERSION = NodeJsVersion("24.16.0")
        private val UNSUPPORTED_VERSION = NodeJsVersion("16.20.2")
        private val WINDOWS_PLATFORM = BuildPlatform("win", "x64")
    }
}
