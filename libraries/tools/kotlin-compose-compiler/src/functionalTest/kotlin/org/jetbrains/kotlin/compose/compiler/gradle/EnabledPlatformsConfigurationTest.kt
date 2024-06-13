/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.testUtils.buildProjectWithMPP
import org.jetbrains.kotlin.compose.compiler.gradle.testUtils.composeOptions
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.tasks.BaseKotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.utils.named
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class EnabledPlatformsConfigurationTest {

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun allKotlinPlatformsAreUsedByDefault() {
        val project = buildProjectWithMPP {
            extensions.getByType<KotlinMultiplatformExtension>().apply {
                jvm()
                js { nodejs() }
                wasmJs()
                wasmWasi()
                linuxX64()

                applyDefaultHierarchyTemplate()
            }
        }

        project.evaluate()

        listOf(
            "compileKotlinMetadata",
            "compileCommonMainKotlinMetadata",
            "compileKotlinJs",
            "compileKotlinJvm",
            "compileKotlinWasmJs",
            "compileKotlinWasmWasi"
        ).forEach {
            assertTrue(
                project.tasks.named<BaseKotlinCompile>(it).get().composeOptions().isNotEmpty(),
                "'$it' task does not contain compose plugin options"
            )
        }

        assertTrue(
            project.tasks.named<KotlinNativeCompile>("compileKotlinLinuxX64").get().composeOptions().isNotEmpty(),
            "'compileKotlinLinuxX64' task does not contain compose plugin options"
        )
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun disableKotlinPlatforms() {
        val project = buildProjectWithMPP {
            extensions.getByType<KotlinMultiplatformExtension>().apply {
                jvm()
                js { nodejs() }
                wasmJs()
                wasmWasi()
                linuxX64()

                applyDefaultHierarchyTemplate()
            }

            extensions.getByType<ComposeCompilerGradlePluginExtension>()
                .targetKotlinPlatforms
                .set(setOf(KotlinPlatformType.jvm))
        }

        project.evaluate()

        assertTrue(
            project.tasks.named<BaseKotlinCompile>("compileKotlinJvm").get().composeOptions().isNotEmpty(),
            "'compileKotlinJvm' task does not contain compose plugin options"
        )

        listOf(
            "compileKotlinMetadata",
            "compileCommonMainKotlinMetadata",
            "compileKotlinJs",
            "compileKotlinWasmJs",
            "compileKotlinWasmWasi"
        ).forEach {
            assertTrue(
                project.tasks.named<BaseKotlinCompile>(it).get().composeOptions().isEmpty(),
                "'$it' task contains compose plugin options"
            )
        }

        assertTrue(
            project.tasks.named<KotlinNativeCompile>("compileKotlinLinuxX64").get().composeOptions().isEmpty(),
            "'compileKotlinLinuxX64' task contains compose plugin options"
        )
    }
}