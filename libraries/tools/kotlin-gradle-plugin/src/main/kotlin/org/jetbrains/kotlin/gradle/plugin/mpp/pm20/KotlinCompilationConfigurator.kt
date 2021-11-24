/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName

interface KotlinCompileTaskConfigurator<in T : KotlinGradleVariant> {
    fun registerCompileTasks(variant: T): TaskProvider<*>
}

object KotlinJvmCompileTaskConfigurator : KotlinCompileTaskConfigurator<KotlinJvmVariant> {
    override fun registerCompileTasks(variant: KotlinJvmVariant): TaskProvider<*> {
        val compilationData = variant.compilationData
        LifecycleTasksManager(variant.project).registerClassesTask(compilationData)
        return KotlinCompilationTaskConfigurator(variant.project).createKotlinJvmCompilationTask(variant, compilationData)
    }
}

object KotlinNativeCompileTaskConfigurator : KotlinCompileTaskConfigurator<KotlinNativeVariantInternal> {
    override fun registerCompileTasks(variant: KotlinNativeVariantInternal): TaskProvider<*> {
        val compilationData = variant.compilationData
        LifecycleTasksManager(variant.project).registerClassesTask(compilationData)
        return KotlinCompilationTaskConfigurator(variant.project).createKotlinNativeCompilationTask(variant, compilationData)
    }
}

// TODO NOW: Find place
object KotlinFragmentJarArtifactElementsConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        val project = fragment.project
        val module = fragment.containingModule
        val jarTaskName = fragment.disambiguateName("jar")
        val jarTask = project.locateOrRegisterTask<Jar>(jarTaskName) {
            it.from(fragment.compilationOutputs.allOutputs)
            it.archiveClassifier.set(dashSeparatedName(fragment.name, module.moduleClassifier))
        }
        project.artifacts.add(configuration.name, jarTask)
    }
}


