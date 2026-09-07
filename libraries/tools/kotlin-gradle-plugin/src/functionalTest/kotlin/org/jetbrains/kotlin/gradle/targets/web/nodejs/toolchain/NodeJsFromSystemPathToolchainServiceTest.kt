/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.nodejs.toolchain

import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.util.assertLogContains
import org.jetbrains.kotlin.gradle.util.withGradleLogCapture
import org.jetbrains.kotlin.konan.target.HostManager
import org.junit.jupiter.api.Assumptions
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFalse

/**
 * Tests [NodeJsFromSystemPathToolchainService] against a fake `node` executable, so that the tests are
 * hermetic and do not depend on the Node.js installed on the machine running them.
 *
 * The fake executable is a shell script, so the tests only run on hosts with a POSIX shell.
 */
class NodeJsFromSystemPathToolchainServiceTest {

    private val project = ProjectBuilder.builder().build()

    private val tempDir: File = createTempDirectory("node-from-system-path-test").toFile()

    private val invocationsLog: File get() = tempDir.resolve("invocations.txt")

    private var registeredServices = 0

    @BeforeTest
    fun setUp() {
        Assumptions.assumeFalse(HostManager.hostIsMingw, "The fake Node.js executable is a shell script")
    }

    @AfterTest
    fun tearDown() {
        tempDir.deleteRecursively()
    }

    @Test
    fun `when the pre-installed Node js is requested, expect its version and platform are detected`() {
        val command = fakeNodeJs(version = "24.16.0", platform = "linux", arch = "x64")
        val service = createService(command)

        val nodeJs = service.request { version("24.16.0") }
        assertEquals(0, invocations(), "Expected no Node.js execution at configuration time")

        val resolved = nodeJs.get()

        assertEquals(1, invocations())
        assertEquals(command.absolutePath, resolved.executable.get())
        assertEquals(NodeJsVersion("24.16.0"), resolved.version.get())
        assertEquals(BuildPlatform("linux", "x64"), resolved.platform.get())
        assertFalse(
            tempDir.resolve(nodeJsDistributionName(NodeJsVersion("24.16.0"), BuildPlatform("linux", "x64"))).exists(),
            "Expected nothing to be downloaded",
        )
    }

    @Test
    fun `when the pre-installed Node js runs on Windows, expect the distribution platform naming`() {
        val service = createService(fakeNodeJs(version = "24.16.0", platform = "win32", arch = "x64"))

        val resolved = service.request().get()

        assertEquals(BuildPlatform("win", "x64"), resolved.platform.get())
    }

    @Test
    fun `when the pre-installed Node js has another version, expect a warning`() {
        val service = createService(fakeNodeJs(version = "22.14.0", platform = "linux", arch = "x64"))

        val logs = withGradleLogCapture { service.request { version("24.16.0") }.get() }

        logs.assertLogContains("does not match the requested version 24.16.0")
    }

    @Test
    fun `when the pre-installed Node js runs on another platform, expect a warning`() {
        val service = createService(fakeNodeJs(version = "24.16.0", platform = "linux", arch = "x64"))

        val logs = withGradleLogCapture {
            service.request { platform.set(BuildPlatform("linux", "arm64")) }.get()
        }

        logs.assertLogContains("runs on linux-x64, but linux-arm64 was requested")
    }

    @Test
    fun `when the configured command cannot be run, expect an actionable failure`() {
        val service = createService(tempDir.resolve("there-is-no-node-here"))

        val failure = assertFails { service.request().get() }

        assertContains(failure.message.orEmpty(), "to detect the pre-installed Node.js")
    }

    private fun invocations(): Int = if (invocationsLog.isFile) invocationsLog.readLines().size else 0

    /**
     * Creates an executable behaving the way `node -p '...'` does for the detection script
     * [NodeJsFromSystemPathToolchainService] runs.
     */
    private fun fakeNodeJs(version: String, platform: String, arch: String): File =
        tempDir.resolve("fake-node-${registeredServices}").apply {
            writeText(
                """
                #!/bin/sh
                echo invoked >> "${invocationsLog.absolutePath}"
                echo "$version $platform $arch"
                """.trimIndent()
            )
            setExecutable(true)
        }

    private fun createService(command: File): NodeJsFromSystemPathToolchainService = project.gradle.sharedServices
        .registerIfAbsent(
            "nodeJsFromSystemPathToolchain${registeredServices++}",
            NodeJsFromSystemPathToolchainService::class.java,
        ) { spec ->
            spec.parameters.command.set(command.absolutePath)
        }
        .get()
}
