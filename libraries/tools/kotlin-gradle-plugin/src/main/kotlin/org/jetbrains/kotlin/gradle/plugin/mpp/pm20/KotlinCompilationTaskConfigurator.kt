/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.Kotlin2JvmSourceSetProcessor
import org.jetbrains.kotlin.gradle.plugin.KotlinNativeTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.addCommonSourcesToKotlinCompileTask
import org.jetbrains.kotlin.gradle.plugin.mpp.addSourcesToKotlinCompileTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinTasksProvider
import org.jetbrains.kotlin.gradle.tasks.withType

open class KotlinCompilationTaskConfigurator(
    private val project: Project
) {
    private val kotlinPluginVersion =
        project.getKotlinPluginVersion() ?: error("Kotlin plugin version not found; the Kotlin plugin must be applied to $project")

    open val variantSourcesProvider: VariantSourcesProvider = VariantSourcesProvider()

    fun createKotlinJvmCompilationTask(
        variant: KotlinGradleVariant,
        compilationData: KotlinCompilationData<*>
    ): TaskProvider<out KotlinCompile> {
        Kotlin2JvmSourceSetProcessor(KotlinTasksProvider(), compilationData, kotlinPluginVersion).run()
        val allSources = variantSourcesProvider.getSourcesFromRefinesClosure(variant)
        val commonSources = variantSourcesProvider.getCommonSourcesFromRefinesClosure(variant)

        // FIXME support custom source file extensions in the two calls below
        addSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { allSources }
        addCommonSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { commonSources }

        return project.tasks.withType<KotlinCompile>().named(compilationData.compileKotlinTaskName)
    }

    fun createKotlinNativeCompilationTask(
        variant: KotlinNativeVariant,
        compilationData: KotlinNativeCompilationData<*>
    ): TaskProvider<KotlinNativeCompile> {
        val compileTask = KotlinNativeTargetConfigurator.createKlibCompilationTask(compilationData)

        val allSources = variantSourcesProvider.getSourcesFromRefinesClosure(variant)
        val commonSources = variantSourcesProvider.getSourcesFromRefinesClosure(variant)

        compileTask.configure {
            it.source(allSources)
            it.commonSources.from(commonSources)
        }

        return compileTask
    }
}