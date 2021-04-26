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
    protected val project: Project
) {
    open val fragmentSourcesProvider: FragmentSourcesProvider = FragmentSourcesProvider()

    open fun getSourcesForFragmentCompilation(fragment: KotlinGradleFragment): MultipleSourceRootsProvider =
        fragmentSourcesProvider.getSourcesFromRefinesClosure(fragment)

    open fun getCommonSourcesForFragmentCompilation(fragment: KotlinGradleFragment): MultipleSourceRootsProvider =
        fragmentSourcesProvider.getCommonSourcesFromRefinesClosure(fragment)

    fun createKotlinJvmCompilationTask(
        fragment: KotlinGradleFragment,
        compilationData: KotlinCompilationData<*>
    ): TaskProvider<out KotlinCompile> {
        Kotlin2JvmSourceSetProcessor(KotlinTasksProvider(), compilationData).run()
        val allSources = getSourcesForFragmentCompilation(fragment)
        val commonSources = getCommonSourcesForFragmentCompilation(fragment)

        // FIXME support custom source file extensions in the two calls below
        addSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { allSources }
        addCommonSourcesToKotlinCompileTask(project, compilationData.compileKotlinTaskName, emptyList()) { commonSources }

        return project.tasks.withType<KotlinCompile>().named(compilationData.compileKotlinTaskName)
    }

    fun createKotlinNativeCompilationTask(
        fragment: KotlinGradleFragment,
        compilationData: KotlinNativeCompilationData<*>
    ): TaskProvider<KotlinNativeCompile> {
        val compileTask = KotlinNativeTargetConfigurator.createKlibCompilationTask(compilationData)

        val allSources = getSourcesForFragmentCompilation(fragment)
        val commonSources = getCommonSourcesForFragmentCompilation(fragment)

        compileTask.configure {
            it.source(allSources)
            it.commonSources.from(commonSources)
        }

        return compileTask
    }
}