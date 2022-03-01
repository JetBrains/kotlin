/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.gradle.api.tasks.TaskProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinGradleVariant

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


