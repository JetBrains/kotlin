/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SharedNpmSubprojectTest {

    @Test
    fun `JS and WasmJS targets publish the package json of every compilation, WasmWASI publishes nothing`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { browser() }
                wasmJs { browser() }
                wasmWasi { nodejs() }
            }
        }
        project.evaluate()

        val js = project.multiplatformExtension.js() as KotlinJsIrTarget
        val wasmJs = project.multiplatformExtension.wasmJs() as KotlinJsIrTarget

        assertEquals(
            js.compilations.map { it.npmProject.packageJsonFile.get().asFile }.sorted(),
            project.configurations.getByName("kotlinNpmSharedPackageJsonFiles").artifacts.files.files.sorted(),
        )
        assertEquals(
            wasmJs.compilations.map { it.npmProject.packageJsonFile.get().asFile }.sorted(),
            project.configurations.getByName("kotlinWasmNpmSharedPackageJsonFiles").artifacts.files.files.sorted(),
        )
        assertEquals(
            listOf("kotlinNpmSharedPackageJsonFiles", "kotlinWasmNpmSharedPackageJsonFiles"),
            project.configurations.names.filter { it.endsWith("NpmSharedPackageJsonFiles") }.sorted(),
        )
    }
}