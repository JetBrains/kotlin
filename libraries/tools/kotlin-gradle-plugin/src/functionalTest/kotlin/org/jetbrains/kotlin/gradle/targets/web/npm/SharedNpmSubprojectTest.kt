/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.artifacts.FileCollectionDependency
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
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.readNpmDependencyReport
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
    return project.layout.buildDirectory.dir("kotlin-npm/$platform/npm-dep-reports").get().asFile
        .resolve(outputModuleName.get())
        .resolve("npm-dep-report.json")
}

class SharedNpmSubprojectTest {

    private fun Project.npmDependenciesReportTask(name: String) = tasks.getByName(name) as KotlinNpmDependencyReportTask

    private fun Project.npmDependencies(taskName: String): CompilationNpmDependencies =
        npmDependenciesReportTask(taskName).compilation

    private fun Project.npmDependenciesReportTaskNames(): List<String> =
        tasks.withType(KotlinNpmDependencyReportTask::class.java).names.sorted()

    private fun Project.npmDependenciesReportsForRootProject(configurationName: String): List<File> =
        configurations.getByName(configurationName).artifacts.files.files.sorted()

    private fun Project.jsTarget() = multiplatformExtension.js() as KotlinJsIrTarget

    private fun Project.wasmJsTarget() = multiplatformExtension.wasmJs() as KotlinJsIrTarget

    @Test
    fun `the root project is offered the npm dependency report of every compilation`() {
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
            project.npmDependenciesReportTaskNames(),
        )
    }

    @Test
    fun `WasmWASI target registers no npm dependency report task`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                wasmWasi {
                    wasmtime()
                }
            }
        }
        project.evaluate()

        assertEquals(listOf("jsNpmDependenciesReport", "jsTestNpmDependenciesReport"), project.npmDependenciesReportTaskNames())
        assertEquals(
            listOf("kotlinNpmDependenciesReportJs"),
            project.configurations.names.filter { it.startsWith("kotlinNpmDependenciesReport") },
        )
        val wasmWasiRuntimeElements = project.configurations.getByName(project.multiplatformExtension.wasmWasi().runtimeElementsConfigurationName)
        assertNull(wasmWasiRuntimeElements.outgoing.variants.findByName("kotlinNpmRuntimeDependenciesWasmWasi"))
    }

    @Test
    fun `dependent projects are offered the npm dependency report of the main compilation only`() {
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
        for ((target, variantName, usageName) in expected) {
            val variant = project.configurations.getByName(target.runtimeElementsConfigurationName).outgoing.variants.getByName(variantName)
            assertEquals(usageName, variant.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name)
            assertEquals(
                listOf(target.compilations.getByName("main").npmDependenciesReportFile()),
                variant.artifacts.files.files.toList(),
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
    fun `versions of one npm package are joined into one range`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                sourceSets.getByName("commonMain").dependencies {
                    npm("is-even", "1.0.0")
                }
                sourceSets.getByName("jsMain").dependencies {
                    npm("is-even", ">1.0.0")
                }
            }
        }
        project.evaluate()

        val task = project.npmDependenciesReportTask("jsNpmDependenciesReport")
        task.compilation.dependencyReports.setFrom()
        task.generate()

        val report = readNpmDependencyReport(task.outputFile.get().asFile)
        assertEquals("test", report.compilation)
        assertEquals(mapOf("is-even" to "1.0.0 >1.0.0"), report.dependencies)
    }

    @Test
    fun `klibs and unpacked klibs declared with files are kept in the npm runtime dependencies resolver`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                sourceSets.getByName("jsMain").dependencies {
                    implementation(files("foo-js.klib", "foo-js"))
                }
            }
        }
        project.evaluate()

        val resolver = project.configurations.getByName("jsNpmRuntimeDependenciesResolver")
        assertEquals(
            listOf(setOf(project.file("foo-js.klib"), project.file("foo-js"))),
            resolver.allDependencies.filterIsInstance<FileCollectionDependency>().map { it.files.files },
        )
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
    fun `the report target is the platform of the compilation`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                wasmJs { nodejs() }
            }
        }
        project.evaluate()

        assertEquals("js", project.npmDependenciesReportTask("jsNpmDependenciesReport").target.get())
        assertEquals("wasm", project.npmDependenciesReportTask("wasmJsNpmDependenciesReport").target.get())
    }

    @Test
    fun `the npm dependency report of a dependency project is an input of the dependent one`() {
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

        val transitive = app.npmDependencies("jsNpmDependenciesReport").dependencyReports
        assertTrue(
            transitive.buildDependencies.getDependencies(null).any { it.name == "jsNpmDependenciesReport" && it.project == lib },
            "app's npm dependency report must be built after lib's",
        )
    }
}
