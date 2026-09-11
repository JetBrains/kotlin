/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.NPM_DEPENDENCIES_REPORT_USAGE
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.SharedNpmPlatform
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.sharedNpmPlatformAttribute
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SharedNpmSubprojectTest {

    private fun setupProject(
        configure: Project.() -> Unit = {},
    ): Project {
        val project = buildProjectWithMPP(
            projectBuilder = {
                withName("demo-project")
            },
        )

        project.kotlin {
            wasmWasi {
                nodejs()
            }
            wasmJs {
                browser()
            }
            js {
                browser()
            }

            sourceSets.commonMain {
                dependencies {
                    implementation(npm("is-even", "1.0.0"))
                }
            }
        }

        configure(project)
        project.evaluate()
        return project
    }

    @Test
    fun `package json configuration is registered for JS and WasmJS but not for WasmWASI`() {
        val project = setupProject()

        for (platform in SharedNpmPlatform.values()) {
            checkNotNull(project.configurations.findByName(platform.reportConfigurationName)) {
                "Expected configuration '${platform.reportConfigurationName}' to exist."
            }
        }

        val reportConfigurationNames = project.configurations.names.filter { it.contains("NpmDependenciesReport") }
        assertEquals(
            SharedNpmPlatform.values().map { it.reportConfigurationName }.sorted().prettyPrinted,
            reportConfigurationNames.sorted().prettyPrinted,
        )
    }

    @Test
    fun `package json configuration is consumable only and carries the report attributes`() {
        val project = setupProject()

        for (platform in SharedNpmPlatform.values()) {
            val configuration = project.configurations.getByName(platform.reportConfigurationName)

            assertTrue(configuration.isCanBeConsumed, "'${configuration.name}' must be consumable.")
            assertFalse(configuration.isCanBeResolved, "'${configuration.name}' must not be resolvable.")

            assertEquals(
                NPM_DEPENDENCIES_REPORT_USAGE,
                configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name,
            )
            assertEquals(
                Category.LIBRARY,
                configuration.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name,
            )
            assertEquals(
                platform.targetId,
                configuration.attributes.getAttribute(sharedNpmPlatformAttribute),
            )
        }
    }

    @Test
    fun `configuration publishes the package json of every compilation`() {
        val project = setupProject()

        val targetsByPlatform = mapOf(
            SharedNpmPlatform.JS to project.multiplatformExtension.js() as KotlinJsIrTarget,
            SharedNpmPlatform.WASM_JS to project.multiplatformExtension.wasmJs() as KotlinJsIrTarget,
        )

        for ((platform, target) in targetsByPlatform) {
            val expectedFiles = target.compilations
                .map { it.npmProject.packageJsonFile.get().asFile }
                .sorted()

            val publishedFiles = project.configurations.getByName(platform.reportConfigurationName)
                .artifacts.files.files
                .sorted()

            assertEquals(expectedFiles.prettyPrinted, publishedFiles.prettyPrinted)
        }
    }

    @Test
    fun `WASI-only project registers no package json configuration`() {
        val project = buildProjectWithMPP(
            projectBuilder = { withName("wasi-only") },
        )
        project.kotlin {
            wasmWasi {
                nodejs()
            }
        }
        project.evaluate()

        for (platform in SharedNpmPlatform.values()) {
            assertNull(project.configurations.findByName(platform.reportConfigurationName))
        }
    }

    @Test
    fun `JS-only project registers no WasmJS configuration`() {
        val project = buildProjectWithMPP(
            projectBuilder = { withName("js-only") },
        )
        project.kotlin {
            js {
                nodejs()
            }
        }
        project.evaluate()

        assertNull(project.configurations.findByName(SharedNpmPlatform.WASM_JS.reportConfigurationName))
        checkNotNull(project.configurations.findByName(SharedNpmPlatform.JS.reportConfigurationName))
    }
}
