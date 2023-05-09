/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.internal.StartParameterInternal
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * Gradle 8.0 has changed internal method return type to `Option.Value<Boolean>` from previous `BuildOption.Value<Boolean>`.
 */
interface ProjectIsolationStartParameterAccessor {
    val isProjectIsolationEnabled: Boolean

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(gradle: Gradle): ProjectIsolationStartParameterAccessor
    }
}

internal class DefaultProjectIsolationStartParameterAccessorVariantFactory :
    ProjectIsolationStartParameterAccessor.Factory {
    override fun getInstance(gradle: Gradle): ProjectIsolationStartParameterAccessor {
        return DefaultProjectIsolationStartParameterAccessor(gradle)
    }
}

internal class DefaultProjectIsolationStartParameterAccessor(
    private val gradle: Gradle
) : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean by lazy {
        (gradle.startParameter as StartParameterInternal).isolatedProjects.get()
    }
}

internal val Project.isProjectIsolationEnabled
    get() = variantImplementationFactory<ProjectIsolationStartParameterAccessor.Factory>()
        .getInstance(gradle)
        .isProjectIsolationEnabled
