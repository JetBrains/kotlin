/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class SharedNpmManagerProjectTest {

    private val root = buildProject(projectBuilder = { withName("root-project") }) {
        plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
    }
    private val jsBrowser = subproject("js-browser") { js { browser() } }
    private val wasmJsBrowser = subproject("wasm-js-browser") { wasmJs { browser() } }
    private val jsNodejs = subproject("js-nodejs") { js { nodejs() } }

    private fun subproject(name: String, configure: KotlinMultiplatformExtension.() -> Unit): ProjectInternal {
        val project = buildProjectWithMPP(projectBuilder = { withParent(root).withName(name) }) { kotlin(configure) }
        root.dependencies.add("kotlinNpmSharedDependencies", root.dependencies.project(mapOf("path" to project.path)))
        root.dependencies.add("kotlinWasmNpmSharedDependencies", root.dependencies.project(mapOf("path" to project.path)))
        project.evaluate()
        return project
    }

    private fun packageJsonFiles(vararg projects: Project): List<File> = projects
        .flatMap { it.multiplatformExtension.targets.filterIsInstance<KotlinJsIrTarget>() }
        .flatMap { it.compilations }
        .map { it.npmProject.packageJsonFile.get().asFile }
        .sorted()

    private fun resolvedFiles(configurationName: String): List<File> =
        root.configurations.getByName(configurationName).incoming.artifactView { it.lenient(true) }.files.files.sorted()

    private fun assembleJsSharedNpmProject(): File {
        (root.tasks.getByName("kotlinSetupSharedNpmProject") as KotlinSetupSharedNpmProjectTask).assemble()
        return root.layout.buildDirectory.dir("js/shared-npm-project").get().asFile
    }

    @Test
    fun `package json files of all compilations are collected per platform`() {
        assertEquals(packageJsonFiles(jsBrowser, jsNodejs), resolvedFiles("kotlinNpmSharedDependenciesResolver"))
        assertEquals(packageJsonFiles(wasmJsBrowser), resolvedFiles("kotlinWasmNpmSharedDependenciesResolver"))
    }

    @Test
    fun `setup task assembles the root and workspace package json files`() {
        val inputs = packageJsonFiles(jsBrowser, jsNodejs)
        inputs.forEachIndexed { index, file -> PackageJson("lib-$index", "1.0.0").saveTo(file) }

        val outputDirectory = assembleJsSharedNpmProject()

        val rootPackageJson = fromSrcPackageJson(outputDirectory.resolve("package.json"))!!
        assertEquals("root-project", rootPackageJson.name)
        assertEquals("unspecified", rootPackageJson.version)
        assertEquals(true, rootPackageJson.private)
        assertEquals(listOf("packages/lib-0", "packages/lib-1", "packages/lib-2", "packages/lib-3"), rootPackageJson.workspaces?.toList())
        inputs.forEachIndexed { index, file ->
            assertEquals(file.readText(), outputDirectory.resolve("packages/lib-$index/package.json").readText())
        }
    }

    @Test
    fun `invalid package json files are skipped`() {
        val (valid, unparsable, unnamed) = packageJsonFiles(jsBrowser, jsNodejs)
        PackageJson("valid", "1.0.0").saveTo(valid)
        unparsable.parentFile.mkdirs()
        unparsable.writeText("{ not json")
        unnamed.parentFile.mkdirs()
        unnamed.writeText("""{ "version": "1.0.0" }""")

        val outputDirectory = assembleJsSharedNpmProject()

        assertEquals(listOf("packages/valid"), fromSrcPackageJson(outputDirectory.resolve("package.json"))!!.workspaces?.toList())
    }
}