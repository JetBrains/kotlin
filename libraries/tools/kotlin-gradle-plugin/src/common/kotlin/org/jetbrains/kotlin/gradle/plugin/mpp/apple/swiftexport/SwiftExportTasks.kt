/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.apple.swiftexport

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.Framework
import org.jetbrains.kotlin.gradle.plugin.mpp.enabledOnCurrentHostForBinariesCompilation
import org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName

internal fun Project.registerSwiftExportEmbedTask(
    framework: Framework,
): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHostForBinariesCompilation()) return null
    return registerSwiftExportEmbedPipelineTask(
        swiftApiModuleName = framework.baseNameProvider,
        taskNamePrefix = framework.taskNamePrefix,
        target = framework.target,
        buildType = framework.buildType,
    )
}

internal fun Project.registerSwiftExportFrameworkTask(
    framework: Framework,
): TaskProvider<out Task>? {
    if (!framework.konanTarget.family.isAppleFamily || !framework.konanTarget.enabledOnCurrentHostForBinariesCompilation()) return null
    return registerSwiftExportFrameworkPipelineTask(
        swiftApiModuleName = framework.baseNameProvider,
        taskNamePrefix = framework.taskNamePrefix,
        target = framework.target,
        buildType = framework.buildType,
    )
}

private val Framework.taskNamePrefix: String
    get() = lowerCamelCaseName(
        target.disambiguationClassifier ?: target.name,
        buildType.getName(),
    )