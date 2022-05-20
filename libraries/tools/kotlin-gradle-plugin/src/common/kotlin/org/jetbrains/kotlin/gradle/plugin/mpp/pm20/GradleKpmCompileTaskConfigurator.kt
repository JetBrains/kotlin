/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.tasks.TaskProvider

interface GradleKpmCompileTaskConfigurator<in T : GradleKpmVariant> {
    fun registerCompileTasks(variant: T): TaskProvider<*>
}

object GradleKpmJvmCompileTaskConfigurator : GradleKpmCompileTaskConfigurator<GradleKpmJvmVariant> {
    override fun registerCompileTasks(variant: GradleKpmJvmVariant): TaskProvider<*> {
        val compilationData = variant.compilationData
        LifecycleTasksManager(variant.project).registerClassesTask(compilationData)
        return GradleKpmCompilationTaskConfigurator(variant.project).createKotlinJvmCompilationTask(variant, compilationData)
    }
}

object GradleKpmNativeCompileTaskConfigurator : GradleKpmCompileTaskConfigurator<GradleKpmNativeVariantInternal> {
    override fun registerCompileTasks(variant: GradleKpmNativeVariantInternal): TaskProvider<*> {
        val compilationData = variant.compilationData
        LifecycleTasksManager(variant.project).registerClassesTask(compilationData)
        return GradleKpmCompilationTaskConfigurator(variant.project).createKotlinNativeCompilationTask(variant, compilationData)
    }
}
