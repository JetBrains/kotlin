/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.attributes.Usage
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.npm.DefaultKotlinNpmDependency
import org.jetbrains.kotlin.gradle.npm.KotlinNpmDependency
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.CompilationNpmDependencies
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSharedPackageJsonTask
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal fun KotlinJsIrCompilation.sharedPackageJsonFile(): File {
    val platform = if (target.platformType == KotlinPlatformType.wasm) "wasm" else "js"
    return project.layout.buildDirectory.dir("kotlin-npm/$platform/shared-package-json").get().asFile
        .resolve(outputModuleName.get())
        .resolve("package.json")
}

class SharedNpmSubprojectTest {

    private fun Project.sharedPackageJsonTask(name: String) = tasks.getByName(name) as KotlinSharedPackageJsonTask

    private fun Project.sharedPackageJsonCompilation(taskName: String, moduleName: String): CompilationNpmDependencies =
        sharedPackageJsonTask(taskName).compilationsDependencies.get().single { it.moduleName.get() == moduleName }

    private fun Project.packageJsonsForRootProject(configurationName: String): List<File> =
        configurations.getByName(configurationName).artifacts.files.files.sorted()

    private fun Project.jsTarget() = multiplatformExtension.js() as KotlinJsIrTarget

    private fun Project.wasmJsTarget() = multiplatformExtension.wasmJs() as KotlinJsIrTarget

    private fun evaluatedProject(name: String = "test", configure: KotlinMultiplatformExtension.() -> Unit): ProjectInternal =
        buildProjectWithMPP(projectBuilder = { withName(name) }) { kotlin(configure) }.also { it.evaluate() }

    @Test
    fun `the root project is offered the package json of every compilation, and nothing for WasmWASI`() {
        val project = evaluatedProject {
            js { browser() }
            wasmJs { browser() }
            wasmWasi { nodejs() }
        }

        assertEquals(
            project.jsTarget().compilations.map { it.sharedPackageJsonFile() }.sorted(),
            project.packageJsonsForRootProject("kotlinNpmSharedPackageJsonFiles"),
        )
        assertEquals(
            project.wasmJsTarget().compilations.map { it.sharedPackageJsonFile() }.sorted(),
            project.packageJsonsForRootProject("kotlinWasmNpmSharedPackageJsonFiles"),
        )
        assertEquals(
            listOf("kotlinNpmSharedPackageJsonFiles", "kotlinWasmNpmSharedPackageJsonFiles"),
            project.configurations.names.filter { it.endsWith("NpmSharedPackageJsonFiles") }.sorted(),
        )
        assertEquals(
            listOf("kotlinSharedPackageJson", "kotlinWasmSharedPackageJson"),
            project.tasks.names.filter { it.endsWith("SharedPackageJson") }.sorted(),
        )
        assertEquals(
            listOf("test", "test-test"),
            project.sharedPackageJsonTask("kotlinSharedPackageJson").compilationsDependencies.get().map { it.moduleName.get() }.sorted(),
        )
    }

    @Test
    fun `WasmWASI only project registers no shared package json task`() {
        val project = evaluatedProject { wasmWasi { nodejs() } }

        assertEquals(emptyList(), project.tasks.names.filter { it.endsWith("SharedPackageJson") })
        assertEquals(emptyList(), project.configurations.names.filter { it.contains("SharedPackageJson") })
    }

    @Test
    fun `dependent projects are offered the package json of the main compilation only`() {
        val project = evaluatedProject {
            js { nodejs() }
            wasmJs { nodejs() }
        }

        val expected = listOf(
            Triple(project.jsTarget(), "jsSharedPackageJsonElements", "kotlinNpmSharedPackageJsonElements"),
            Triple(project.wasmJsTarget(), "wasmJsSharedPackageJsonElements", "kotlinWasmNpmSharedPackageJsonElements"),
        )
        for ((target, configurationName, usageName) in expected) {
            val configuration = project.configurations.getByName(configurationName)
            assertEquals(usageName, configuration.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name)
            assertEquals(
                listOf(target.compilations.getByName("main").sharedPackageJsonFile()),
                configuration.artifacts.files.files.toList(),
            )
        }
    }

    @Test
    fun `directly declared npm dependencies are collected from the compilation and its source sets`() {
        val project = evaluatedProject {
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

        val isEven = DefaultKotlinNpmDependency("is-even", "1.0.0", KotlinNpmDependency.Scope.NORMAL)
        val isOdd = DefaultKotlinNpmDependency("is-odd", "2.0.0", KotlinNpmDependency.Scope.DEV)
        val cowsay = DefaultKotlinNpmDependency("cowsay", "9.9.9", KotlinNpmDependency.Scope.PEER)

        assertEquals(setOf(isEven, isOdd), project.sharedPackageJsonCompilation("kotlinSharedPackageJson", "test").directDependencies.get())
        assertEquals(setOf(isEven, isOdd, cowsay), project.sharedPackageJsonCompilation("kotlinSharedPackageJson", "test-test").directDependencies.get())
    }

    @Test
    fun `tool dependencies of the test task are collected as devDependencies`() {
        val project = evaluatedProject {
            js { browser() }
            wasmJs { browser() }
        }

        for (taskName in listOf("kotlinSharedPackageJson", "kotlinWasmSharedPackageJson")) {
            val testTools = project.sharedPackageJsonCompilation(taskName, "test-test").toolDependencies.get()
            assertTrue("karma" in testTools.map { it.name }, "karma expected in $taskName test tools: $testTools")
            assertTrue(testTools.all { it.scope == KotlinNpmDependency.Scope.DEV }, "tool dependencies must be devDependencies: $testTools")
        }
    }

    @Test
    fun `package json metadata of the compilation is passed to the task`() {
        val project = evaluatedProject {
            js { nodejs() }
        }
        project.version = "1.2.3"

        val main = project.sharedPackageJsonCompilation("kotlinSharedPackageJson", "test")
        assertEquals("kotlin/test.js", main.main.get())
        assertNull(main.types.orNull)
        assertEquals("1.2.3", main.packageVersion.get())
    }
}
