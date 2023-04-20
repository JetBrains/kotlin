/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.test.assertTrue

@OtherGradlePluginTests
class K2KotlinxSerializationIT : KGPBaseTest() {
    @DisplayName("Compile common code to metadata with kotlinx.serialization and K2")
    @GradleTest
    fun `test kotlinxSerializationMppK2`(gradleVersion: GradleVersion) {
        project("kotlinxSerializationMppK2", gradleVersion) {
            build(":compileCommonMainKotlinMetadata") {
                assertTasksExecuted(":compileCommonMainKotlinMetadata")
            }
        }
    }

    @DisplayName("Compile code with kotlinx.serialization with K2 against K1. KT-57941")
    @GradleTest
    fun `test kotlinx serialization K2 against K1`(gradleVersion: GradleVersion) {
        project("kotlinxSerializationK2AgainstK1", gradleVersion) {
            build(":app:run") {
                assertTasksExecuted(":app:run")
            }
        }
    }

    @DisplayName("Compile code with kotlinx.serialization K2 against old K1 library without enum factory support (KT-57704).")
    @GradleTest
    fun `test kotlinx serialization K2 against K1 library`(gradleVersion: GradleVersion, @TempDir tempDir: Path) {
        project("kotlinxSerializationK2AgainstK1Lib/lib", gradleVersion, localRepoDir = tempDir) {
            build(":publish") {
                assertTasksExecuted(":publish")
                val publishedLib = tempDir.resolve("com/example/serialization_lib/serialization_lib/1.0")
                assertDirectoryExists(publishedLib)
                assertTrue(publishedLib.listDirectoryEntries().isNotEmpty())
            }
        }

        project("kotlinxSerializationK2AgainstK1Lib/app", gradleVersion, localRepoDir = tempDir) {
            build(":run") {
                assertTasksExecuted(":run")
            }
        }
    }

    @DisplayName("Compile production executable with kotlinx.serialization to JS. KT-57690, KT-57807")
    @GradleTest
    fun `test kotlinx serialization compile to JS`(gradleVersion: GradleVersion) {
        project("kotlinxSerializationK2WithJs", gradleVersion) {
            build(":compileProductionExecutableKotlinJs")
        }
    }

    @DisplayName("Compile test sourceset with kotlinx.serialization to JS. KT-57781")
    @GradleTest
    fun `test kotlinx serialization compile test source set to JS`(gradleVersion: GradleVersion) {
        project("kotlinxSerializationK2WithJs", gradleVersion) {
            build(":compileTestKotlinJs")
        }
    }

    @DisplayName("Compile MPP project to JS kotlinx.serialization and K2")
    @GradleTest
    fun `test kotlinx serialization mpp to JS`(gradleVersion: GradleVersion) {
        project("kotlinxSerializationMppK2", gradleVersion) {
            build(":compileKotlinJs")
        }
    }
}
