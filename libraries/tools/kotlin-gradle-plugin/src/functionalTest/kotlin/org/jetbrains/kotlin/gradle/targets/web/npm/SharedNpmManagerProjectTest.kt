/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

class SharedNpmManagerProjectTest {

    @Test
    fun `npm dependency reports of all compilations are collected per platform`() {
        val root = buildProject(
            projectBuilder = {
                withName("root")
            }
        )
        root.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)

        val jsBrowser = buildKotlinProject(parent = root, name = "js-browser") {
            js {
                browser()
            }
        }
        val jsNodejs = buildKotlinProject(parent = root, name = "js-nodejs") {
            js {
                nodejs()
            }
        }
        val wasmJsBrowser = buildKotlinProject(parent = root, name = "wasm-js-browser") {
            wasmJs {
                browser()
            }
        }

        for (project in listOf(jsBrowser, jsNodejs, wasmJsBrowser)) {
            root.addSharedNpmDependency("kotlinJsNpmSharedDependencies", project)
            root.addSharedNpmDependency("kotlinWasmNpmSharedDependencies", project)
            project.evaluate()
        }

        assertEquals(
            reportFiles(jsBrowser, jsNodejs),
            root.resolvedReports("kotlinNpmDependenciesReportJsResolver"),
        )
        assertEquals(
            reportFiles(wasmJsBrowser),
            root.resolvedReports("kotlinNpmDependenciesReportWasmResolver"),
        )
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

        val lib = buildKotlinProject(parent = root, name = "lib") {
            js {
                nodejs()
            }
        }
        manager.addSharedNpmDependency("kotlinJsNpmSharedDependencies", lib)
        lib.evaluate()

        assertEquals(
            reportFiles(lib),
            manager.resolvedReports("kotlinNpmDependenciesReportJsResolver"),
        )

        val setupTask = manager.tasks.getByName("kotlinJsSetupSharedNpmProject") as KotlinSetupSharedNpmProjectTask

        val expectedOutputDirectory = manager.layout.buildDirectory
            .dir("js/shared-npm-project")
            .get()
            .asFile
        assertEquals(expectedOutputDirectory, setupTask.outputDirectory.get().asFile)

        assertEquals("npm-manager", setupTask.rootPackageName.get())
    }
}

private fun buildKotlinProject(
    parent: Project,
    name: String,
    configure: KotlinMultiplatformExtension.() -> Unit,
): ProjectInternal {
    val project = buildProjectWithMPP(
        projectBuilder = {
            withParent(parent)
            withName(name)
        }
    )
    project.kotlin(configure)
    return project
}

private fun Project.addSharedNpmDependency(configurationName: String, project: Project) {
    val projectDependency = dependencies.project(mapOf("path" to project.path))
    dependencies.add(configurationName, projectDependency)
}

private fun Project.resolvedReports(resolverName: String): List<File> =
    configurations.getByName(resolverName)
        .incoming
        .artifactView { it.lenient(true) }
        .files
        .files
        .sorted()

private fun reportFiles(vararg projects: Project): List<File> =
    projects
        .flatMap { it.multiplatformExtension.targets.filterIsInstance<KotlinJsIrTarget>() }
        .flatMap { it.compilations }
        .map { it.npmDependenciesReportFile() }
        .sorted()
