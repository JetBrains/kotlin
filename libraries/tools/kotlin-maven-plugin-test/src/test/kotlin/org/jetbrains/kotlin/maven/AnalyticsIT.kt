/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.maven

import org.jetbrains.kotlin.maven.test.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import java.io.File
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.io.path.createDirectories

@DisplayName("Analytics (FUS statistics)")
class AnalyticsIT : KotlinMavenTestBase() {

    private fun profileFiles(kotlinUserHome: Path): List<File> =
        kotlinUserHome.resolve("kotlin-profile").toFile()
            .listFiles { f -> f.name.endsWith(".kotlin-profile") }
            ?.toList()
            .orEmpty()

    @MavenTest
    @DisplayName("Build writes a profile file under the directory set by kotlin.user.home")
    fun testProfileFileIsWritten(mavenVersion: TestVersions.Maven) {
        val kotlinHome = tmpDir.resolve("kotlin-home").createDirectories()

        testProject("test-enable-extensions", mavenVersion) {
            build("compile", "-Dkotlin.user.home=$kotlinHome")
        }

        val files = profileFiles(kotlinHome)
        assertEquals(1, files.size, "Expected exactly one profile file, got: $files")
        val content = files.single().readText()
        assertTrue(content.contains("KOTLIN_COMPILER_VERSION="), content)
        assertTrue(content.contains("NUMBER_OF_SUBPROJECTS=1"), content)
        assertTrue(content.contains("COMPILATION_STARTED=true"), content)
        assertTrue(Regex("COMPILATIONS_COUNT=[1-9]\\d*").containsMatchIn(content), content)
        assertTrue(content.contains("BUILD_FAILED=false"), content)
    }

    @MavenTest
    @DisplayName("Analytics can be disabled")
    fun testDisabled(mavenVersion: TestVersions.Maven) {
        val kotlinHome = tmpDir.resolve("kotlin-home").createDirectories()

        testProject("test-enable-extensions", mavenVersion) {
            build("compile", "-Dkotlin.user.home=$kotlinHome", "-Denable_kotlin_performance_profile=false")
        }

        assertTrue(profileFiles(kotlinHome).isEmpty())
    }

    @MavenTest
    @DisplayName("Parallel builds each collect their own analytics")
    fun testParallelBuilds(mavenVersion: TestVersions.Maven) {
        val kotlinHome = tmpDir.resolve("kotlin-home").createDirectories()
        val projects = listOf(
            "test-enable-extensions",
            "test-smart-defaults-enabled",
            "test-smart-defaults-jvm-target-multimodule",
        )

        val executor = Executors.newFixedThreadPool(projects.size)
        try {
            projects.map { name ->
                executor.submit(Callable {
                    testProject(name, mavenVersion) {
                        build("compile", "-Dkotlin.user.home=$kotlinHome")
                    }
                })
            }.forEach { it.get() }
        } finally {
            executor.shutdown()
        }

        val files = profileFiles(kotlinHome)
        assertEquals(projects.size, files.size, "Expected a profile file per build, got: $files")
        for (file in files) {
            val content = file.readText()
            assertTrue(content.contains("COMPILATION_STARTED=true"), content)
            assertTrue(content.contains("BUILD_FAILED=false"), content)
        }
        // Each file belongs to a distinct build
        assertEquals(files.size, files.map { it.readLines().first() }.toSet().size)
    }
}
