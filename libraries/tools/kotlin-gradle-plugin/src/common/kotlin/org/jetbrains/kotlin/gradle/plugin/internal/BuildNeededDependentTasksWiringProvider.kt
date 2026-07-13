/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.targets.KotlinTargetSideEffect

/**
 * `buildNeeded` and `buildDependent` tasks were deprecated in Gradle 9.6.0 release:
 * https://docs.gradle.org/current/userguide/upgrading_version_9.html#deprecate_build_needed_build_dependents_tasks
 */
internal interface BuildNeededDependentTasksWiringProvider {

    fun wireSideEffect(): KotlinTargetSideEffect

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): BuildNeededDependentTasksWiringProvider
    }
}

internal class DefaultBuildNeededDependentTaskWiringProvider : BuildNeededDependentTasksWiringProvider {
    override fun wireSideEffect(): KotlinTargetSideEffect = KotlinTargetSideEffect {
        // no-op
    }

    class Factory : BuildNeededDependentTasksWiringProvider.Factory {
        override fun getInstance() = DefaultBuildNeededDependentTaskWiringProvider()
    }
}

internal val Project.buildNeededDependentTasksWiringProvider
    get() = variantImplementationFactory<BuildNeededDependentTasksWiringProvider.Factory>()
        .getInstance()
