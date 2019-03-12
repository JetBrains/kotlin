/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetProcessor
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider

class KotlinJvmTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJvmCompilation>(
    project,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJvmCompilation>): KotlinCompilationFactory<KotlinJvmCompilation> =
        KotlinJvmCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.jvm

    override fun buildCompilationProcessor(compilation: KotlinJvmCompilation): KotlinSourceSetProcessor<*> {
        val tasksProvider = KotlinTasksProvider(compilation.target.targetName)
        return Kotlin2JvmSourceSetProcessor(project, tasksProvider, compilation, kotlinPluginVersion)
    }

    companion object {
        const val PRESET_NAME = "jvm"
    }
}
