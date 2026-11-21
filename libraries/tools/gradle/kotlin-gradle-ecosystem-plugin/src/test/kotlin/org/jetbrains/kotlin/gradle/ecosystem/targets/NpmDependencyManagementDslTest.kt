/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(
    ExperimentalNpmDependencyManagementDsl::class,
    ExperimentalKotlinGradlePluginApi::class,
    ExperimentalWasmDsl::class,
    DelicateKotlinGradlePluginApi::class,
    ExperimentalNodeJsToolchainDsl::class,
)

package org.jetbrains.kotlin.gradle.ecosystem.targets

import org.gradle.api.initialization.Settings
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.ecosystem.npm.*
import org.jetbrains.kotlin.gradle.ecosystem.util.applyKotlinEcosystemPlugin
import org.jetbrains.kotlin.gradle.ecosystem.util.kotlin
import org.jetbrains.kotlin.gradle.ecosystem.util.mockGradleSettings
import org.junit.jupiter.api.DynamicContainer
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.TestFactory
import kotlin.test.assertEquals

class NpmDependencyManagementDslTest {

    @TestFactory
    fun defaults(): List<DynamicContainer> {
        val settings = mockGradleSettings()
        return settings.npmDependencyManagements().map { target ->
            dynamicContainer(
                "${target::class.simpleName}",
                listOf(
                    dynamicTest("ignoreScripts") {
                        assertEquals(true, target.ignoreScripts.orNull)
                    },
                    dynamicTest("sharedNpmDependenciesMode") {
                        assertEquals(SharedNpmDependenciesMode.SHARED_IN_ROOT_PROJECT, target.sharedNpmDependenciesMode.orNull)
                    },
                    dynamicTest("nodeJsVersionForDependencyResolution") {
                        assertEquals("24.22.1", target.nodeJsVersionForDependencyResolution.orNull?.version)
                    },
                    dynamicTest("overrides are empty") {
                        assertEquals(emptyMap(), target.overrides.versions.orNull)
                    },
                    dynamicTest("lockFile.directory") {
                        val expectedLockFileDirectory = when (target) {
                            is JsNpmDependencyManagementDsl -> settings.layout.settingsDirectory.dir("kotlin-js-store")
                            is WasmJsNpmDependencyManagementDsl -> settings.layout.settingsDirectory.dir("kotlin-js-store/wasm")
                            else -> error("Unexpected target type: ${target::class.simpleName}")
                        }
                        assertEquals(expectedLockFileDirectory.asFile, target.lockFile.directory.orNull?.asFile)
                    },
                    dynamicTest("lockFile.mismatchReport") {
                        assertEquals(NpmLockFileMismatchReport.FAIL, target.lockFile.mismatchReport.orNull)
                    },
                    dynamicTest("lockFile.reportNewLockFile") {
                        assertEquals(false, target.lockFile.reportNewLockFile.orNull)
                    },
                    dynamicTest("lockFile.autoReplace") {
                        assertEquals(false, target.lockFile.autoReplace.orNull)
                    },
                ),
            )
        }
    }

    companion object {
        private fun Settings.npmDependencyManagements(): List<NpmDependencyManagementDsl> {
            settings.applyKotlinEcosystemPlugin()
            return listOf(
                settings.kotlin.js.npmDependencyManagement,
                settings.kotlin.wasmJs.npmDependencyManagement,
            )
        }
    }
}
