/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.CompilationNpmDependencies
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinNpmDependencyReportTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun KotlinJsIrCompilation.npmDependenciesReportFile(): File {
    val platform = if (target.platformType == KotlinPlatformType.wasm) "wasm" else "js"
    return project.layout.buildDirectory.dir("kotlin-npm/$platform/shared-package-json").get().asFile
        .resolve(outputModuleName.get())
        .resolve("package.json")
}

class SharedNpmSubprojectTest {

    private fun Project.npmDependenciesReportTask(name: String) = tasks.getByName(name) as KotlinNpmDependencyReportTask

    private fun Project.npmDependencies(taskName: String): CompilationNpmDependencies =
        npmDependenciesReportTask(taskName).compilation

    private fun Project.npmDependenciesReportsForRootProject(configurationName: String): List<File> =
        configurations.getByName(configurationName).artifacts.files.files.sorted()

    private fun Project.jsTarget() = multiplatformExtension.js() as KotlinJsIrTarget

    private fun Project.wasmJsTarget() = multiplatformExtension.wasmJs() as KotlinJsIrTarget

    @Test
    fun `the root project is offered the package json of every compilation`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { browser() }
                wasmJs { browser() }
            }
        }
        project.evaluate()

        assertEquals(
            project.jsTarget().compilations.map { it.npmDependenciesReportFile() }.sorted(),
            project.npmDependenciesReportsForRootProject("kotlinNpmDependenciesReportJs"),
        )
        assertEquals(
            project.wasmJsTarget().compilations.map { it.npmDependenciesReportFile() }.sorted(),
            project.npmDependenciesReportsForRootProject("kotlinNpmDependenciesReportWasm"),
        )
        assertEquals(
            listOf("kotlinNpmDependenciesReportJs", "kotlinNpmDependenciesReportWasm"),
            project.configurations.names.filter { it.startsWith("kotlinNpmDependenciesReport") }.sorted(),
        )
        assertEquals(
            listOf("jsNpmDependenciesReport", "jsTestNpmDependenciesReport", "wasmJsNpmDependenciesReport", "wasmJsTestNpmDependenciesReport"),
            project.tasks.names.filter { it.endsWith("NpmDependenciesReport") }.sorted(),
        )
    }

    @Test
    fun `WasmWASI only project registers no shared package json task`() {
        val project = buildProjectWithMPP {
            kotlin {
                wasmWasi {
                    wasmtime()
                }
            }
        }
        project.evaluate()

        assertEquals(emptyList(), project.tasks.names.filter { it.endsWith("NpmDependenciesReport") })
        assertEquals(emptyList(), project.configurations.names.filter { it.contains("NpmDependenciesReport") || it.contains("NpmRuntimeDependencies") })
    }

    @Test
    fun `dependent projects are offered the package json of the main compilation only`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                wasmJs { nodejs() }
            }
        }
        project.evaluate()

        val expected = listOf(
            Triple(project.jsTarget(), "kotlinNpmRuntimeDependenciesJs", "kotlinNpmRuntimeDependenciesJs"),
            Triple(project.wasmJsTarget(), "kotlinNpmRuntimeDependenciesWasmJs", "kotlinNpmRuntimeDependenciesWasm"),
        )
        for ((target, configurationName, usageName) in expected) {
            val configuration = project.configurations.getByName(configurationName)
            assertEquals(usageName, configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name)
            assertEquals(
                listOf(target.compilations.getByName("main").npmDependenciesReportFile()),
                configuration.artifacts.files.files.toList(),
            )
        }
    }

    @Test
    fun `directly declared npm dependencies are collected from the compilation and its source sets`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                sourceSets.getByName("commonMain").dependencies {
                    npm("is-even", "1.0.0")
                }
                sourceSets.getByName("jsMain").dependencies {
                    npmDev("is-odd", "2.0.0")
                }
                sourceSets.getByName("jsTest").dependencies {
                    npmPeer("cowsay", "9.9.9")
                }
            }
        }
        project.evaluate()

        val isEven = DefaultKotlinNpmDependency("is-even", "1.0.0", KotlinNpmDependency.Scope.NORMAL)
        val isOdd = DefaultKotlinNpmDependency("is-odd", "2.0.0", KotlinNpmDependency.Scope.DEV)
        val cowsay = DefaultKotlinNpmDependency("cowsay", "9.9.9", KotlinNpmDependency.Scope.PEER)

        assertEquals(setOf(isEven, isOdd), project.npmDependencies("jsNpmDependenciesReport").directDependencies.get())
        assertEquals(setOf(isEven, isOdd, cowsay), project.npmDependencies("jsTestNpmDependenciesReport").directDependencies.get())
    }

    @Test
    fun `tool dependencies of the test task are collected as devDependencies`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { browser() }
                wasmJs { browser() }
            }
        }
        project.evaluate()

        for (taskName in listOf("jsTestNpmDependenciesReport", "wasmJsTestNpmDependenciesReport")) {
            val testTools = project.npmDependencies(taskName).toolDependencies.get()
            assertTrue("karma" in testTools.map { it.name }, "karma expected in $taskName test tools: $testTools")
            assertTrue(testTools.all { it.scope == KotlinNpmDependency.Scope.DEV }, "tool dependencies must be devDependencies: $testTools")
        }
    }

    @Test
    fun `package json metadata of the compilation is passed to the task`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
            }
        }
        project.evaluate()
        project.version = "1.2.3"

        val main = project.npmDependencies("jsNpmDependenciesReport")
        assertEquals("kotlin/test.js", main.main.get())
        assertNull(main.types.orNull)
        assertEquals("1.2.3", main.packageVersion.get())
    }

    @Test
    fun `the shared package json of a dependency project is an input of the dependent one`() {
        val root = buildProject(projectBuilder = { withName("root") })
        val lib = buildProjectWithMPP(projectBuilder = { withParent(root).withName("lib") }) {
            kotlin { js { nodejs() } }
        }
        val app = buildProjectWithMPP(projectBuilder = { withParent(root).withName("app") }) {
            kotlin {
                js { nodejs() }
                sourceSets.getByName("jsMain").dependencies {
                    implementation(project(":lib"))
                }
            }
        }
        lib.evaluate()
        app.evaluate()

        val transitive = app.npmDependencies("jsNpmDependenciesReport").transitiveArtifacts
        assertTrue(
            transitive.buildDependencies.getDependencies(null).any { it.name == "jsNpmDependenciesReport" && it.project == lib },
            "app's shared package.json must be built after lib's",
        )
    }
}
