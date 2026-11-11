/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.NpmDependencyReport
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.writeTo
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SharedNpmManagerProjectTest {

    private val root = buildProject(projectBuilder = { withName("root-project") }) {
        plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
    }
    private val jsBrowser = subproject("js-browser") { js { browser() } }
    private val wasmJsBrowser = subproject("wasm-js-browser") { wasmJs { browser() } }
    private val jsNodejs = subproject("js-nodejs") { js { nodejs() } }

    private fun subproject(name: String, configure: KotlinMultiplatformExtension.() -> Unit): ProjectInternal {
        val project = buildProjectWithMPP(projectBuilder = { withParent(root).withName(name) }) { kotlin(configure) }
        root.dependencies.add("kotlinJsNpmSharedDependencies", root.dependencies.project(mapOf("path" to project.path)))
        root.dependencies.add("kotlinWasmNpmSharedDependencies", root.dependencies.project(mapOf("path" to project.path)))
        project.evaluate()
        return project
    }

    private fun reportFiles(vararg projects: Project): List<File> = projects
        .flatMap { it.multiplatformExtension.targets.filterIsInstance<KotlinJsIrTarget>() }
        .flatMap { it.compilations }
        .map { it.npmDependenciesReportFile() }
        .sorted()

    private fun resolvedFiles(configurationName: String): List<File> =
        root.configurations.getByName(configurationName).incoming.artifactView { it.lenient(true) }.files.files.sorted()

    private fun assembleJsSharedNpmProject(): File {
        (root.tasks.getByName("kotlinJsSetupSharedNpmProject") as KotlinSetupSharedNpmProjectTask).assemble()
        return root.layout.buildDirectory.dir("js/shared-npm-project").get().asFile
    }

    @Test
    fun `npm dependency reports of all compilations are collected per platform`() {
        assertEquals(reportFiles(jsBrowser, jsNodejs), resolvedFiles("kotlinNpmDependenciesReportJsResolver"))
        assertEquals(reportFiles(wasmJsBrowser), resolvedFiles("kotlinNpmDependenciesReportWasmResolver"))
    }

    @Test
    fun `setup task assembles the root and workspace package json files`() {
        val inputs = reportFiles(jsBrowser, jsNodejs)
        inputs.forEachIndexed { index, file ->
            NpmDependencyReport("js", "lib-$index", dependencies = mapOf("is-even" to "1.$index.0")).writeTo(file)
        }

        val outputDirectory = assembleJsSharedNpmProject()

        val rootPackageJson = fromSrcPackageJson(outputDirectory.resolve("package.json"))!!
        assertEquals("root-project", rootPackageJson.name)
        assertEquals("0.0.0-unspecified", rootPackageJson.version)
        assertEquals(true, rootPackageJson.private)
        assertEquals(listOf("packages/lib-0", "packages/lib-1", "packages/lib-2", "packages/lib-3"), rootPackageJson.workspaces?.toList())
        inputs.indices.forEach { index ->
            val workspace = fromSrcPackageJson(outputDirectory.resolve("packages/lib-$index/package.json"))!!
            assertEquals("lib-$index", workspace.name)
            assertEquals("0.0.0-unspecified", workspace.version)
            assertEquals(mapOf("is-even" to "1.$index.0"), workspace.dependencies.toMap())
        }
    }

    @Test
    fun `an unparsable npm dependency report fails the setup task`() {
        val report = reportFiles(jsBrowser).first()
        report.parentFile.mkdirs()
        report.writeText("{ not json")

        assertFailsWith<GradleException> { assembleJsSharedNpmProject() }
    }

    @Test
    fun `an npm dependency report without a compilation name fails the setup task`() {
        NpmDependencyReport("js", "").writeTo(reportFiles(jsBrowser).first())

        assertFailsWith<GradleException> { assembleJsSharedNpmProject() }
    }

    @Test
    fun `the plugin can be applied to a non-root project`() {
        val root = buildProject(
            projectBuilder = {
                withName("root")
            }
        )

        val manager = buildProject(
            projectBuilder = {
                withParent(root)
                withName("npm-manager")
            }
        )
        manager.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)

        val lib = buildProjectWithMPP(
            projectBuilder = {
                withParent(root)
                withName("lib")
            }
        )
        lib.kotlin {
            js {
                nodejs()
            }
        }

        val libDependency = manager.dependencies.project(mapOf("path" to lib.path))
        manager.dependencies.add("kotlinJsNpmSharedDependencies", libDependency)

        lib.evaluate()

        val resolvedReports = manager.configurations.getByName("kotlinNpmDependenciesReportJsResolver")
            .incoming
            .artifactView { it.lenient(true) }
            .files
            .files
            .sorted()
        assertEquals(reportFiles(lib), resolvedReports)

        val setupTask = manager.tasks.getByName("kotlinJsSetupSharedNpmProject") as KotlinSetupSharedNpmProjectTask

        val expectedOutputDirectory = manager.layout.buildDirectory
            .dir("js/shared-npm-project")
            .get()
            .asFile
        assertEquals(expectedOutputDirectory, setupTask.outputDirectory.get().asFile)

        assertEquals("npm-manager", setupTask.rootPackageName.get())
    }
}
