/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compose.compiler.gradle

import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.compose.compiler.gradle.testUtils.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.utils.named
import org.junit.jupiter.api.Test
import kotlin.test.assertTrue

class JsAndWasmCompilationIntegrationTest {

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun shouldDisableSignatureClashChecksForJsAndWasm() {
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
            "compileKotlinJvm",
            "compileKotlinLinuxX64",
        ).forEach {
            assertTrue(
                project.tasks.named<KotlinCompilationTask<*>>(it).get().compilerOptions.freeCompilerArgs.get()
                    .none { it == "-Xklib-enable-signature-clash-checks=false" },
                "'$it' task freeCompilerArgs has \"-Xklib-enable-signature-clash-checks=false\""
            )
        }

        listOf(
            "compileKotlinJs",
            "compileKotlinWasmJs",
            "compileKotlinWasmWasi",
        ).forEach {
            assertTrue(
                project.tasks.named<KotlinCompilationTask<*>>(it).get().compilerOptions
                    .freeCompilerArgs.get().contains("-Xklib-enable-signature-clash-checks=false"),
                "'$it' task freeCompilerArgs does not have \"-Xklib-enable-signature-clash-checks=false\""
            )
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun shouldNotDisableSignatureClashChecksForJsAndWasmIfComposeNotEnabled() {
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
                .targetKotlinPlatforms.set(
                    KotlinPlatformType.values()
                        .filterNot { it == KotlinPlatformType.wasm || it == KotlinPlatformType.js }
                        .asIterable()
                )
        }

        project.evaluate()

        listOf(
            "compileKotlinMetadata",
            "compileCommonMainKotlinMetadata",
            "compileKotlinJvm",
            "compileKotlinLinuxX64",
            "compileKotlinJs",
            "compileKotlinWasmJs",
            "compileKotlinWasmWasi",
        ).forEach {
            assertTrue(
                project.tasks.named<KotlinCompilationTask<*>>(it).get().compilerOptions.freeCompilerArgs.get()
                    .none { it == "-Xklib-enable-signature-clash-checks=false" },
                "'$it' task freeCompilerArgs has \"-Xklib-enable-signature-clash-checks=false\""
            )
        }
    }

    @OptIn(ExperimentalWasmDsl::class)
    @Test
    fun shouldNotDisableSignatureClashChecksForWasmIfComposeNotEnabled() {
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
                .targetKotlinPlatforms.set(
                    KotlinPlatformType.values()
                        .filterNot { it == KotlinPlatformType.wasm }
                        .asIterable()
                )
        }

        project.evaluate()

        listOf(
            "compileKotlinMetadata",
            "compileCommonMainKotlinMetadata",
            "compileKotlinJvm",
            "compileKotlinLinuxX64",
            "compileKotlinWasmJs",
            "compileKotlinWasmWasi",
        ).forEach {
            assertTrue(
                project.tasks.named<KotlinCompilationTask<*>>(it).get().compilerOptions.freeCompilerArgs.get()
                    .none { it == "-Xklib-enable-signature-clash-checks=false" },
                "'$it' task freeCompilerArgs has \"-Xklib-enable-signature-clash-checks=false\""
            )
        }

        assertTrue(
            project.tasks.named<KotlinCompilationTask<*>>("compileKotlinJs").get().compilerOptions
                .freeCompilerArgs.get().contains("-Xklib-enable-signature-clash-checks=false"),
            "'compileKotlinJs' task freeCompilerArgs does not have \"-Xklib-enable-signature-clash-checks=false\""
        )
    }
}