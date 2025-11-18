/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.internal

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.ide.Idea222Api
import org.jetbrains.kotlin.gradle.plugin.ide.ideaImportDependsOn
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.CheckXcodeTargetsConfigurationTask
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.xcode.tasks.ConvertPbxprojToJsonTask
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.konan.target.KonanTarget
import java.io.File

private const val XCODE_TASK_GROUP = "xcode"

/**
 * Registers two tasks:
 * 1. A producer task that converts the `.pbxproj` file to JSON.
 * 2. A consumer task that reads the JSON and performs the configuration check.
 * This ensures the expensive `plutil` command is only run when the Xcode project changes,
 * while the cheap validation task runs whenever the JSON changes, consistently re-issuing warnings.
 */
internal val XcodeConfigurationSetupAction = KotlinProjectSetupCoroutine {
    // 1. Check if there are any apple targets with frameworks. If not, the check is not needed.
    if (!shouldSetupXcodeConfiguration()) {
        return@KotlinProjectSetupCoroutine
    }

    // 2. Check for the Xcode project path. If it's not found, log an informational message.
    val projectPath = project.xcodeProjectPath
    if (projectPath == null) {
        val searchedPaths = project.xcodeProjectSearchedPaths
        logger.info(
            "Kotlin Xcode project checker: .xcodeproj directory not found. Searched in:\n" +
                    searchedPaths.joinToString("\n") { " - ${it.path}" } +
                    "\nSkipping task registration."
        )
        return@KotlinProjectSetupCoroutine
    }

    // 3. If everything is in place, register the tasks.
    val appleTargets = getAppleTargetsWithFrameworkBinaries()
    val convertTask = registerConvertPbxprojToJsonTask(projectPath)
    registerCheckXcodeTargetsConfigurationTask(appleTargets, projectPath, convertTask)
}

private suspend fun Project.shouldSetupXcodeConfiguration(): Boolean {
    val targets = multiplatformExtension
        .awaitTargets()
        .filterIsInstance<KotlinNativeTarget>()
        .filter { it.konanTarget.family.isAppleFamily }

    if (targets.isEmpty()) return false

    val hasBinaries = targets.flatMap { it.binaries }.filterIsInstance<Framework>().isNotEmpty()
    if (!hasBinaries) return false

    return project.xcodeProjectPath != null
}

private suspend fun Project.getAppleTargetsWithFrameworkBinaries(): List<KonanTarget> {
    return multiplatformExtension
        .awaitTargets()
        .filterIsInstance<KotlinNativeTarget>()
        .filter { target ->
            target.konanTarget.family.isAppleFamily &&
                    target.binaries.filterIsInstance<Framework>().isNotEmpty()
        }
        .map { it.konanTarget }
}

private fun Project.registerConvertPbxprojToJsonTask(
    projectPath: File,
): TaskProvider<ConvertPbxprojToJsonTask> {
    return locateOrRegisterTask(ConvertPbxprojToJsonTask.TASK_NAME) {
        it.group = XCODE_TASK_GROUP
        it.description = "Converts .pbxproj file to JSON for inspection"
        it.pbxprojFile.set(projectPath.resolve("project.pbxproj"))
        it.jsonFile.set(layout.buildDirectory.file("xcode-check/project.json"))
    }
}

private fun Project.registerCheckXcodeTargetsConfigurationTask(
    targets: List<KonanTarget>,
    projectPath: File,
    convertTask: TaskProvider<ConvertPbxprojToJsonTask>,
) {
    locateOrRegisterTask<CheckXcodeTargetsConfigurationTask>(
        CheckXcodeTargetsConfigurationTask.TASK_NAME,
        invokeWhenRegistered = {
            @OptIn(Idea222Api::class)
            ideaImportDependsOn(this)
        },
        configureTask = {
            group = "xcode"
            description = "Checks for configuration mismatches between Xcode and Kotlin Gradle project"

            appleTargets.set(targets)
            xcodeProjectPath.set(projectPath)
            pbxprojJson.set(convertTask.flatMap { it.jsonFile })
        }
    )
}

private val Project.xcodeProjectSearchedPaths: List<File>
    get() {
        val commonPath = "iosApp/iosApp.xcodeproj"
        return listOf(
            layout.projectDirectory.asFile.resolve(commonPath),
            rootDir.resolve(commonPath)
        )
    }

private val Project.xcodeProjectPath: File?
    get() = xcodeProjectSearchedPaths.firstOrNull { it.exists() }