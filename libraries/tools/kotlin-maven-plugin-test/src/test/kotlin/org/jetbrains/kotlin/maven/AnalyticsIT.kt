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

        // Environment
        assertTrue(content.contains("EXECUTED_FROM_IDEA=false"), content)
        assertTrue(content.contains("OS_TYPE="), content)
        assertTrue(Regex("CPU_NUMBER_OF_CORES=[1-9]\\d*").containsMatchIn(content), content)
        assertTrue(Regex("MAVEN_JVM_HEAP_SIZE=\\d+").containsMatchIn(content), content)
        // Versions
        assertTrue(content.contains("MAVEN_VERSION="), content)
        assertTrue(content.contains("KOTLIN_MAVEN_PLUGIN_VERSION="), content)
        assertTrue(Regex("PROJECT_PATH=[0-9a-f]{64}").containsMatchIn(content), content)
        // Project walk
        assertTrue(content.contains("NUMBER_OF_MODULES=1"), content)
        assertTrue(content.contains("STATISTICS_VISIT_ALL_PROJECTS_OVERHEAD="), content)
        // Compilation
        assertTrue(content.contains("KOTLIN_BTA_USED=true"), content)
        assertTrue(content.contains("KOTLIN_COMPILER_EXECUTION_POLICY=daemon"), content)
        // Build finish
        assertTrue(Regex("MAVEN_BUILD_DURATION=\\d+").containsMatchIn(content), content)
        assertTrue(Regex("MAVEN_EXECUTION_DURATION=\\d+").containsMatchIn(content), content)
        assertTrue(Regex("BUILD_FINISH_TIME=\\d+").containsMatchIn(content), content)
    }

    @MavenTest
    @DisplayName("In-process execution policy is reported")
    fun testInProcessPolicy(mavenVersion: TestVersions.Maven) {
        val kotlinHome = tmpDir.resolve("kotlin-home").createDirectories()

        testProject("test-enable-extensions", mavenVersion) {
            build("compile", "-Dkotlin.user.home=$kotlinHome", "-Dkotlin.compiler.daemon=false")
        }

        val content = profileFiles(kotlinHome).single().readText()
        assertTrue(content.contains("KOTLIN_COMPILER_EXECUTION_POLICY=in-process"), content)
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
