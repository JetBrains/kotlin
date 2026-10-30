/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.js.ir

import com.google.gson.Gson
import com.google.gson.JsonObject
import org.gradle.api.Project
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.KotlinBrowserBundler
import org.jetbrains.kotlin.gradle.targets.js.npm.tasks.KotlinImportMapGenerateTask
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.jetbrains.kotlin.gradle.utils.getFile
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals

/**
 * The development server mounts the npm root package directory under a URL prefix.
 *
 * This prefix must be exactly the URL path a browser requests for an npm dependency,
 * and that path is defined by the development import map of [KotlinImportMapGenerateTask].
 * These tests generate a real import map and resolve its paths with [DevServerWorkAction],
 * so the two sides can't diverge unnoticed.
 */
class WasmDevServerNpmDependenciesTest {

    @Test
    fun `npm root package directory is mounted under its root relative path`() {
        val project = wasmProject()

        assertEquals("build/wasm", project.devServerTask().npmRootPackagePrefix.get())
    }

    @Test
    fun `npm dependencies are served under the import map paths`() {
        assertNpmDependencyIsServed(wasmProject())
    }

    @Test
    fun `npm dependencies are served under the import map paths in a subproject`() {
        val rootProject = buildProject(projectBuilder = { withName("root-project") })

        assertNpmDependencyIsServed(
            wasmProject(projectBuilder = { withParent(rootProject).withName("app") })
        )
    }

    @Test
    fun `npm dependencies are served under the import map paths when the build directory is outside the project`() {
        assertNpmDependencyIsServed(
            wasmProject(
                preApplyCode = {
                    layout.buildDirectory.set(projectDir.parentFile.resolve("out/${project.name}"))
                }
            )
        )
    }

    @Test
    fun `npm dependencies are served under the import map paths in a subproject with the build directory outside the project`() {
        val rootProject = buildProject(projectBuilder = { withName("root-project") })

        assertNpmDependencyIsServed(
            wasmProject(
                projectBuilder = { withParent(rootProject).withName("app") },
                preApplyCode = {
                    rootProject.layout.buildDirectory.set(rootProject.projectDir.parentFile.resolve("out/root-project"))
                    layout.buildDirectory.set(projectDir.parentFile.resolve("out/${project.name}"))
                }
            )
        )
    }

    /**
     * Asserts that the development server of [project] serves an npm dependency
     * at the path its development import map points to.
     */
    private fun assertNpmDependencyIsServed(project: ProjectInternal) {
        val devServer = project.devServerTask()
        val npmRootPackageDirectory = devServer.npmRootPackageDirectory.getFile()
        val npmModuleFile = installNpmPackage(npmRootPackageDirectory)
        val npmRootPackagePrefix = devServer.npmRootPackagePrefix.get()

        val importMapPath = project.generateImportMap().getValue(NPM_PACKAGE_NAME)

        val servedFile = DevServerWorkAction.resolveFile(
            requestPathOf(importMapPath),
            DevServerWorkAction.servedDirectories(
                project.contentDirectory(),
                npmRootPackageDirectory,
                npmRootPackagePrefix,
                project.rootDir,
            ),
        )

        assertEquals(
            npmModuleFile.canonicalFile,
            servedFile?.canonicalFile,
            "The import map refers to the npm dependency as '$importMapPath', " +
                    "so the development server must serve it at '${requestPathOf(importMapPath)}', " +
                    "but the npm root package directory is mounted at '$npmRootPackagePrefix'",
        )
    }

    private fun wasmProject(
        projectBuilder: ProjectBuilder.() -> Unit = {},
        preApplyCode: Project.() -> Unit = {},
    ): ProjectInternal = buildProjectWithMPP(projectBuilder, preApplyCode) {
        kotlin {
            wasmJs {
                binaries.executable()
                browser(KotlinBrowserBundler.NONE)
            }
        }
    }.also { it.evaluate() }

    private fun Project.devServerTask(): KotlinWasmDevServerTaskImpl =
        tasks.withType(KotlinWasmDevServerTaskImpl::class.java).single()

    /**
     * Returns the directory the link task syncs the application into.
     *
     * It is not taken from [KotlinWasmDevServerTaskImpl.contentDirectory],
     * because the output of a task can't be queried before the task has run.
     */
    private fun Project.contentDirectory(): File =
        layout.buildDirectory.dir("compileSync/wasmJs/main/developmentExecutable/kotlin").get().asFile

    /**
     * Creates an npm package as if it had been installed by a package manager.
     *
     * Returns the file the package resolves to.
     */
    private fun installNpmPackage(npmRootPackageDirectory: File): File {
        val packageDirectory = npmRootPackageDirectory.resolve("node_modules/$NPM_PACKAGE_NAME")
        val moduleFile = packageDirectory.resolve(NPM_PACKAGE_MAIN)
        moduleFile.parentFile.mkdirs()
        moduleFile.writeText("export const value = 42;")
        packageDirectory.resolve("package.json")
            .writeText("""{ "name": "$NPM_PACKAGE_NAME", "main": "$NPM_PACKAGE_MAIN" }""")

        return moduleFile
    }

    /**
     * Runs the development import map task and returns the generated module name to module path mapping.
     */
    private fun Project.generateImportMap(): Map<String, String> {
        val importMapTask = tasks.withType(KotlinImportMapGenerateTask::class.java)
            .single { it.name.endsWith(DEV_IMPORT_MAP_TASK_SUFFIX) }
        importMapTask.importMapFile.getFile().parentFile.mkdirs()
        importMapTask.generate()

        val importMap = Gson().fromJson(importMapTask.importMapFile.getFile().readText(), JsonObject::class.java)
        return importMap.getAsJsonObject("imports")
            .entrySet()
            .associate { (moduleName, modulePath) -> moduleName to modulePath.asString }
    }

    /**
     * Returns the path of the request a browser sends for the import map entry [modulePath].
     *
     * Import map entries are resolved as URLs against the page, so dot segments are removed,
     * and `..` segments of an absolute path can't escape the root of the server.
     */
    private fun requestPathOf(modulePath: String): String {
        val segments = ArrayDeque<String>()
        modulePath.split('/').forEach { segment ->
            when (segment) {
                "", "." -> Unit
                ".." -> segments.removeLastOrNull()
                else -> segments.addLast(segment)
            }
        }

        return segments.joinToString("/")
    }

    private companion object {
        const val NPM_PACKAGE_NAME = "foo"
        const val NPM_PACKAGE_MAIN = "dist/foo.mjs"

        /**
         * Distinguishes the import map of the development server from the one of the production distribution.
         */
        const val DEV_IMPORT_MAP_TASK_SUFFIX = "Server"
    }
}
