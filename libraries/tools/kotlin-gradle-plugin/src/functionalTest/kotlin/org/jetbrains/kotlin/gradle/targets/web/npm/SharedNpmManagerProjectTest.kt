/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalWasmDsl::class)

package org.jetbrains.kotlin.gradle.targets.web.npm

import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.attributes.Category
import org.gradle.api.attributes.Usage
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.js.npm.PackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.fromSrcPackageJson
import org.jetbrains.kotlin.gradle.targets.js.npm.npmProject
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.KotlinSetupSharedNpmProjectTask
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.NPM_DEPENDENCIES_REPORT_USAGE
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.SharedNpmPlatform
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.assembleSharedNpmProject
import org.jetbrains.kotlin.gradle.targets.web.npm.shared.sharedNpmPlatformAttribute
import org.jetbrains.kotlin.gradle.testing.prettyPrinted
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.kotlin
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SharedNpmManagerProjectTest {

    @Test
    fun `plugin creates the manager configurations and setup tasks per platform`() {
        val root = buildProject()
        root.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)

        for (platform in SharedNpmPlatform.values()) {
            val shared = root.configurations.getByName(platform.sharedDependenciesConsumableConfigurationName)
            assertFalse(shared.isCanBeConsumed, "'${shared.name}' must not be consumable.")
            assertFalse(shared.isCanBeResolved, "'${shared.name}' must not be resolvable.")

            val resolver = root.configurations.getByName(platform.sharedDependenciesResolvableConfigurationName)
            assertTrue(resolver.isCanBeResolved, "'${resolver.name}' must be resolvable.")
            assertFalse(resolver.isCanBeConsumed, "'${resolver.name}' must not be consumable.")
            assertTrue(resolver.extendsFrom.contains(shared), "'${resolver.name}' must extend '${shared.name}'.")
            assertEquals(
                NPM_DEPENDENCIES_REPORT_USAGE,
                resolver.attributes.getAttribute(Usage.USAGE_ATTRIBUTE)?.name,
            )
            assertEquals(
                Category.LIBRARY,
                resolver.attributes.getAttribute(Category.CATEGORY_ATTRIBUTE)?.name,
            )
            assertEquals(platform.targetId, resolver.attributes.getAttribute(sharedNpmPlatformAttribute))

            val setupTask = root.tasks.named(platform.setupTaskName, KotlinSetupSharedNpmProjectTask::class.java).get()
            assertEquals(
                root.layout.buildDirectory.dir("${platform.rootDirName}/shared-npm-project").get().asFile,
                setupTask.outputDirectory.get().asFile,
            )
        }
    }

    @Test
    fun `plugin application is idempotent`() {
        val root = buildProject()
        root.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
        root.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)

        assertEquals(
            1,
            root.configurations.names.count { it == SharedNpmPlatform.JS.sharedDependenciesConsumableConfigurationName },
        )
    }

    @Test
    fun `plugin fails when applied to a non-root project`() {
        val root = buildProject()
        val sub = buildProject(projectBuilder = { withParent(root).withName("sub") })

        assertFailsWith<IllegalStateException> {
            KotlinSharedNpmProjectPlugin().apply(sub)
        }
    }

    @Test
    fun `declared subprojects are collected`() {
        val root = buildProject(projectBuilder = { withName("root-project") })
        val subA = buildProjectWithMPP(projectBuilder = { withParent(root).withName("lib-a") })
        val subB = buildProjectWithMPP(projectBuilder = { withParent(root).withName("lib-b") })
        buildProject(projectBuilder = { withParent(root).withName("lib-c") })

        root.plugins.apply(KotlinSharedNpmProjectPlugin::class.java)
        for (platform in SharedNpmPlatform.values()) {
            for (path in listOf(":lib-a", ":lib-b", ":lib-c")) {
                root.dependencies.add(
                    platform.sharedDependenciesConsumableConfigurationName,
                    root.dependencies.project(mapOf("path" to path)),
                )
            }
        }

        subA.kotlin {
            js { nodejs() }
        }
        subB.kotlin {
            js { nodejs() }
            wasmJs { nodejs() }
        }
        subA.evaluate()
        subB.evaluate()

        for (platform in SharedNpmPlatform.values()) {
            val declaredPaths = root.configurations
                .getByName(platform.sharedDependenciesConsumableConfigurationName)
                .dependencies
                .map { (it as ProjectDependency).path }
                .sorted()
            assertEquals(listOf(":lib-a", ":lib-b", ":lib-c").prettyPrinted, declaredPaths.prettyPrinted)
        }

        val expectedJsFiles = listOf(subA, subB)
            .flatMap { sub ->
                (sub.multiplatformExtension.js() as KotlinJsIrTarget).compilations
                    .map { it.npmProject.packageJsonFile.get().asFile }
            }
            .sorted()
        val resolvedJsFiles = root.configurations
            .getByName(SharedNpmPlatform.JS.sharedDependenciesResolvableConfigurationName)
            .incoming.artifactView { it.lenient(true) }.files.files
            .sorted()
        assertEquals(expectedJsFiles.prettyPrinted, resolvedJsFiles.prettyPrinted)

        val expectedWasmFiles = (subB.multiplatformExtension.wasmJs() as KotlinJsIrTarget).compilations
            .map { it.npmProject.packageJsonFile.get().asFile }
            .sorted()
        val resolvedWasmFiles = root.configurations
            .getByName(SharedNpmPlatform.WASM_JS.sharedDependenciesResolvableConfigurationName)
            .incoming.artifactView { it.lenient(true) }.files.files
            .sorted()
        assertEquals(expectedWasmFiles.prettyPrinted, resolvedWasmFiles.prettyPrinted)
    }

    @Test
    fun `assembleSharedNpmProject writes workspace and root package json files`(@TempDir workDir: File) {
        val libA = workDir.resolve("inputs/lib-a.json")
        PackageJson("lib-a", "1.0.0").apply {
            dependencies["is-even"] = "1.0.0"
        }.saveTo(libA)

        val libB = workDir.resolve("inputs/lib-b.json")
        PackageJson("lib-b", "1.0.0").apply {
            dependencies["cowsay"] = "9.9.9"
        }.saveTo(libB)

        val outputDirectory = workDir.resolve("out")
        assembleSharedNpmProject(
            packageJsonFiles = listOf(libA, libB),
            rootPackageName = "root-project",
            rootPackageVersion = "2.0.0",
            outputDirectory = outputDirectory,
        )

        val rootPackageJson = checkNotNull(fromSrcPackageJson(outputDirectory.resolve("package.json")))
        assertEquals("root-project", rootPackageJson.name)
        assertEquals("2.0.0", rootPackageJson.version)
        assertEquals(true, rootPackageJson.private)
        assertEquals(
            listOf("packages/lib-a", "packages/lib-b").prettyPrinted,
            rootPackageJson.workspaces?.toList()?.prettyPrinted,
        )

        val copiedLibA = checkNotNull(fromSrcPackageJson(outputDirectory.resolve("packages/lib-a/package.json")))
        assertEquals("lib-a", copiedLibA.name)
        assertEquals(mapOf("is-even" to "1.0.0"), copiedLibA.dependencies)

        val copiedLibB = checkNotNull(fromSrcPackageJson(outputDirectory.resolve("packages/lib-b/package.json")))
        assertEquals(mapOf("cowsay" to "9.9.9"), copiedLibB.dependencies)
    }
}
