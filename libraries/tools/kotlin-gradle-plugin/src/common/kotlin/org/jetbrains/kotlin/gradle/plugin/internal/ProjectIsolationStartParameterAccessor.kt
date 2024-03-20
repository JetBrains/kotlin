/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import org.jetbrains.kotlin.gradle.utils.newInstance
import javax.inject.Inject

/**
 * Gradle 8.0 has changed the internal method return type to `Option.Value<Boolean>` from previous `BuildOption.Value<Boolean>`.
 *
 * Gradle 8.5 has introduced an official API via [BuildFeatures] service.
 */
interface ProjectIsolationStartParameterAccessor {
    val isProjectIsolationEnabled: Boolean
    val isProjectIsolationRequested: Boolean

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): ProjectIsolationStartParameterAccessor
    }
}

internal abstract class DefaultProjectIsolationStartParameterAccessor @Inject constructor(
    buildFeatures: BuildFeatures
) : ProjectIsolationStartParameterAccessor {
    override val isProjectIsolationEnabled: Boolean = buildFeatures.isolatedProjects.active.orElse(false).get()
    override val isProjectIsolationRequested: Boolean = buildFeatures.isolatedProjects.requested.orElse(false).get()

    internal class Factory : ProjectIsolationStartParameterAccessor.Factory {
        override fun getInstance(project: Project): ProjectIsolationStartParameterAccessor = project
            .objects
            .newInstance<DefaultProjectIsolationStartParameterAccessor>()
    }
}

internal val Project.isProjectIsolationEnabled
    get() = variantImplementationFactory<ProjectIsolationStartParameterAccessor.Factory>()
        .getInstance(this)
        .isProjectIsolationEnabled

internal val Project.isProjectIsolationRequested
    get() = variantImplementationFactory<ProjectIsolationStartParameterAccessor.Factory>()
        .getInstance(this)
        .isProjectIsolationRequested
