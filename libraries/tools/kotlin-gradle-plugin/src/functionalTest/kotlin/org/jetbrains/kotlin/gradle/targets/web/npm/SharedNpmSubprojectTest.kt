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
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.encodedVersionSpec
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.CompilationNpmDependencies
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinNpmDependencyReportTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Disabled
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
            listOf(
                "jsNpmDependenciesReport",
                "jsTestNpmDependenciesReport",
                "wasmJsNpmDependenciesReport",
                "wasmJsTestNpmDependenciesReport"
            ),
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
        val wasmWasiRuntimeElements =
            project.configurations.getByName(project.multiplatformExtension.wasmWasi().runtimeElementsConfigurationName)
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

//        val isEven = npm("is-even", "1.0.0")
//        val isOdd = npmDev("is-odd", "2.0.0")
//        val cowsay = npmPeer("cowsay", "9.9.9")

        val expectedMain =
            buildMap {
                put("dev", mapOf("is-odd" to "2.0.0"))
                put("normal", mapOf("is-even" to "1.0.0"))
            }
        assertNpmDepsEqual(
            expectedMain,
            project.npmDependencies("jsNpmDependenciesReport").directUserDependencies.get()
        )

        val expectedTest = buildMap {
            putAll(expectedMain)
            put("peer", mapOf("cowsay" to "9.9.9"))
        }
        assertNpmDepsEqual(
            expectedTest,
            project.npmDependencies("jsTestNpmDependenciesReport").directUserDependencies.get()
        )
    }

    @Test
    fun `npm dependencies declared in Gradle configurations are collected`() {
        val project = buildProjectWithMPP {
            kotlin {
                js { nodejs() }
                sourceSets.getByName("jsMain").dependencies {
                    npm("is-even", "1.0.0")
                    implementation(npm("left-pad", "1.3.0"))
                    implementation(@Suppress("DEPRECATION") devNpm("is-odd", "2.0.0"))
                }
            }
        }
        project.evaluate()

        val expected =
            buildMap {
                put("normal", mapOf("is-even" to "1.0.0", "left-pad" to "1.3.0"))
                put("dev", mapOf("is-odd" to "2.0.0"))
            }

        assertNpmDepsEqual(
            expected,
            project.npmDependencies("jsNpmDependenciesReport").directUserDependencies.get(),
        )
        assertNpmDepsEqual(
            expected,
            project.npmDependencies("jsTestNpmDependenciesReport").directUserDependencies.get(),
        )
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
            val testTools = project.npmDependencies(taskName).npmToolingDependencies.get()
            assertTrue("karma" in testTools.map { it.name }, "karma expected in $taskName test tools: $testTools")
            assertTrue(
                testTools.all { it.dependencyType == KotlinNpmDependency.DependencyType.DEV },
                "tool dependencies must be devDependencies: $testTools"
            )
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
    @Disabled("will be fixed by KT-91337")
    fun `collecting the npm dependencies of a test compilation does not compile main`() {
        val project = buildProjectWithMPP {
            kotlin { js { nodejs() } }
        }
        project.evaluate()

        val mainCompileTask = project.jsTarget().compilations.getByName("main").compileKotlinTaskName
        val reportBuildDependencies = project.npmDependencies("jsTestNpmDependenciesReport").dependencyReports
            .buildDependencies.getDependencies(null).map { it.name }
        assertTrue(
            mainCompileTask !in reportBuildDependencies,
            "jsTestNpmDependenciesReport must not depend on $mainCompileTask: $reportBuildDependencies",
        )
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

    companion object {

        /**
         * Asserts [actual] matches [expected], given as
         * dependency scope (`normal`, `dev`, `peer`, `optional`) → package name → encoded version spec.
         *
         * [KotlinNpmDependency] holds [org.gradle.api.provider.Provider]s, which are compared by identity,
         * so compare their values instead.
         *
         * Both are rendered as multi-line strings, in their existing order, for readable failure diffs.
         */
        private fun assertNpmDepsEqual(expected: Map<String, Map<String, String>>, actual: Set<KotlinNpmDependency>) {
            val actualByScope = actual
                .groupBy { it.dependencyType.name.lowercase() }
                .mapValues { (_, dependencies) -> dependencies.associate { it.name to it.encodedVersionSpec().orNull } }

            fun renderNpmDeps(dependenciesByScope: Map<String, Map<String, String?>>): String =
                dependenciesByScope.entries.joinToString("\n") { (scope, dependencies) ->
                    "$scope:\n" + dependencies.entries.joinToString("\n") { (name, versionSpec) -> "  $name $versionSpec" }
                }

            assertEquals(renderNpmDeps(expected), renderNpmDeps(actualByScope))
        }
    }
}
