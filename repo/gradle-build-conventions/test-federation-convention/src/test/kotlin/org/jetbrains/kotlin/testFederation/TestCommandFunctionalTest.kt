/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.testFederation

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.seconds

class TestCommandFunctionalTest {

    @Test
    fun `test - commit command contributes to changed and affected domains independently`(@TempDir projectDir: Path) {
        with(TestProject(projectDir)) {
            initializeProject()
            commitEmpty("^test: CommonBackend")

            val result = runInferAffectedDomains()

            assertEquals(TaskOutcome.SUCCESS, result.task(":inferAffectedDomains")?.outcome)
            assertChangedDomains(result, setOf(Domain.CommonBackend))
            assertAffectedDomains(result, setOf(Domain.CommonBackend))
            assertEquals("", projectDir.resolve(".test-federation.diff.txt").readText())
        }
    }

    @Test
    fun `test - commit command does not expand alongside file-based changes`(@TempDir projectDir: Path) {
        with(TestProject(projectDir)) {
            initializeProject()

            val changedFile = "compiler/build-tools/changed.txt"
            writeFile(changedFile)
            commitFile("^test: CommonBackend", changedFile)

            val result = runInferAffectedDomains()

            assertEquals(TaskOutcome.SUCCESS, result.task(":inferAffectedDomains")?.outcome)
            assertChangedDomains(result, setOf(Domain.BuildToolsApi, Domain.CommonBackend))
            assertAffectedDomains(result, setOf(Domain.BuildToolsApi, Domain.IntelliJ, Domain.CommonBackend))
            assertEquals(changedFile, projectDir.resolve(".test-federation.diff.txt").readText().trim())
        }
    }

    private fun TestProject.initializeProject() {
        val buildGradleContent = """
            import org.jetbrains.kotlin.testFederation.TestFederationInferAffectedDomainsTask

            plugins {
                id("test-federation-convention") apply false
            }

            tasks.register<TestFederationInferAffectedDomainsTask>("inferAffectedDomains")
            """.trimIndent()

        projectDir.resolve("build.gradle.kts").writeText(buildGradleContent)
        git("init")
        git("config", "user.name", "Test User")
        git("config", "user.email", "test@example.com")
        git("add", "build.gradle.kts")
        git("commit", "-m", "initial commit")
        git("update-ref", "refs/remotes/origin/master", "HEAD")
    }

    private fun TestProject.writeFile(path: String): String {
        projectDir.resolve(path).apply {
            parent.createDirectories()
            writeText("changed")
        }
        return path
    }

    private fun TestProject.commitEmpty(message: String) {
        git("commit", "-m", message, "--allow-empty")
    }

    private fun TestProject.commitFile(message: String, changedFile: String) {
        git("add", changedFile)
        git("commit", "-m", message)
    }

    private fun TestProject.runInferAffectedDomains(): BuildResult {
        val gradleUserHome = System.getenv("GRADLE_USER_HOME") ?: error("Missing 'GRADLE_USER_HOME' environment variable")
        return GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withEnvironment(cleanEnvironment())
            .withPluginClasspath()
            .withTestKitDir(File(gradleUserHome))
            .forwardOutput()
            .withArguments(
                "inferAffectedDomains",
                "-P$TEST_FEDERATION_ENABLED_KEY=true",
                "-Dorg.gradle.daemon.idletimeout=${10.seconds.inWholeMilliseconds}",
            )
            .build()
    }

    private fun TestProject.git(vararg arguments: String) {
        val process = ProcessBuilder("git", *arguments)
            .directory(projectDir.toFile())
            .redirectErrorStream(true)
            .start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        val exitCode = process.waitFor()
        check(exitCode == 0) { "git ${arguments.joinToString(" ")} failed with exit code $exitCode:\n$output" }
    }

    private fun assertChangedDomains(result: BuildResult, domains: Set<Domain>) {
        assertContains(
            result.output,
            "##teamcity[setParameter name='$TEST_FEDERATION_CHANGED_DOMAINS_KEY' value='${domains.toArgumentString()}']",
        )
    }

    private fun assertAffectedDomains(result: BuildResult, domains: Set<Domain>) {
        assertContains(
            result.output,
            "##teamcity[setParameter name='$TEST_FEDERATION_AFFECTED_DOMAINS_KEY' value='${domains.toArgumentString()}']",
        )
    }
}

private class TestProject(val projectDir: Path)
