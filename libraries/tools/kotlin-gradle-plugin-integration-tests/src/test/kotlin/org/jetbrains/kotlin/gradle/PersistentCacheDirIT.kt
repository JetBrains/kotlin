/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.GradleTest
import org.jetbrains.kotlin.gradle.testbase.JvmGradlePluginTests
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.TestProject
import org.jetbrains.kotlin.gradle.testbase.addKgpToBuildScriptCompilationClasspath
import org.jetbrains.kotlin.gradle.testbase.build
import org.jetbrains.kotlin.gradle.testbase.buildScriptInjection
import org.jetbrains.kotlin.gradle.testbase.project
import org.jetbrains.kotlin.gradle.testbase.projectPersistentCache
import org.jetbrains.kotlin.gradle.testbase.source
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.isDirectory
import kotlin.io.path.pathString
import kotlin.io.path.writeText
import kotlin.test.assertTrue

@DisplayName("Kotlin project persistent cache directory")
@JvmGradlePluginTests
class PersistentCacheDirIT : KGPBaseTest() {
    @TempDir
    lateinit var tempDir: Path

    @GradleTest
    @DisplayName("default .kotlin directory is created")
    fun testDefaultPersistentDir(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            configureMultiModuleJvmProject(
                kotlinProjectPersistentDir = null,
            )

            build(":app:compileKotlin")

            assertKotlinPersistentDirCreated(projectPersistentCache)
        }
    }

    @GradleTest
    @DisplayName("project-local kotlin.project.persistent.dir is resolved relative to the root project")
    fun testProjectLocalPersistentDir(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            val persistentDir = "custom-kotlin-persistent-dir"
            configureMultiModuleJvmProject(
                kotlinProjectPersistentDir = persistentDir,
            )

            build(":app:compileKotlin")

            assertKotlinPersistentDirCreated(projectPath.resolve(persistentDir))
        }
    }

    @GradleTest
    @DisplayName("absolute kotlin.project.persistent.dir is used as is")
    fun testAbsolutePersistentDir(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            val persistentDir = tempDir.resolve("absolute-kotlin-persistent-dir")
            configureMultiModuleJvmProject(
                kotlinProjectPersistentDir = persistentDir.pathString,
            )

            build(":app:compileKotlin")

            assertKotlinPersistentDirCreated(persistentDir)
        }
    }

    @GradleTest
    @DisplayName("relative kotlin.project.persistent.dir outside the project is resolved relative to the root project")
    fun testRelativeOutsideProjectPersistentDir(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            val persistentDir = "../relative-outside-kotlin-persistent-dir"
            configureMultiModuleJvmProject(
                kotlinProjectPersistentDir = persistentDir,
            )

            build(":app:compileKotlin")

            assertKotlinPersistentDirCreated(projectPath.resolve(persistentDir).normalize())
        }
    }

    @GradleTest
    @DisplayName("tilde in kotlin.project.persistent.dir is not expanded to user home")
    fun testTildePersistentDirIsTreatedAsProjectRelativePath(gradleVersion: GradleVersion) {
        project("empty", gradleVersion) {
            configureMultiModuleJvmProject(
                kotlinProjectPersistentDir = "~/.kotlin-project-persistent-dir",
            )

            build(":app:compileKotlin")

            assertKotlinPersistentDirCreated(projectPath.resolve("~/.kotlin-project-persistent-dir"))
        }
    }

    private fun TestProject.configureMultiModuleJvmProject(
        kotlinProjectPersistentDir: String?,
    ) {
        if (kotlinProjectPersistentDir != null) {
            gradleProperties.writeText(
                """
            kotlin.project.persistent.dir=${kotlinProjectPersistentDir.replace(File.separatorChar, '/')}
            
            """.trimIndent()
            )
        }

        addKgpToBuildScriptCompilationClasspath()

        includeOtherProjectAsSubmodule("empty", newSubmoduleName = "lib") {
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.jvm")
            }
            kotlinSourcesDir().source("Lib.kt") { """fun lib(): String = "lib"""" }
        }

        includeOtherProjectAsSubmodule("empty", newSubmoduleName = "app") {
            buildScriptInjection {
                project.plugins.apply("org.jetbrains.kotlin.jvm")
                project.dependencies.add("implementation", project.dependencies.project(mapOf("path" to ":lib")))
            }
            kotlinSourcesDir().source("App.kt") { """fun app(): String = lib()""" }
        }
    }

    private fun assertKotlinPersistentDirCreated(persistentDir: Path) {
        assertTrue(
            persistentDir.exists() && persistentDir.isDirectory(),
            "Expected Kotlin project persistent directory to be created: $persistentDir"
        )
        assertTrue(
            persistentDir.resolve("sessions").exists() && persistentDir.resolve("sessions").isDirectory(),
            "Expected Kotlin sessions directory to be created under: $persistentDir"
        )
    }
}
