/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.utils

import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.jetbrains.kotlin.gradle.plugin.extraProperties
import org.jetbrains.kotlin.gradle.plugin.getOrNull
import javax.inject.Inject

/**
 * Provides access to Gradle [BuildFeatures] enabled in the current build.
 */
internal interface BuildFeaturesAccessor {
    val isConfigurationCacheEnabled: Boolean
    val isConfigurationCacheRequested: Boolean
    val isProjectIsolationEnabled: Boolean
    val isProjectIsolationRequested: Boolean
}

internal abstract class DefaultBuildFeaturesAccessor @Inject constructor(
    buildFeatures: BuildFeatures,
) : BuildFeaturesAccessor {
    override val isConfigurationCacheEnabled: Boolean = buildFeatures.configurationCache.active.orElse(false).get()
    override val isConfigurationCacheRequested: Boolean = buildFeatures.configurationCache.requested.orElse(false).get()
    override val isProjectIsolationEnabled: Boolean = buildFeatures.isolatedProjects.active.orElse(false).get()
    override val isProjectIsolationRequested: Boolean = buildFeatures.isolatedProjects.requested.orElse(false).get()
}

private const val EXTRA_KEY = "kgpBuildFeatures"
private val Project.buildFeatures: BuildFeaturesAccessor
    get() = extraProperties.getOrNull(EXTRA_KEY) as? DefaultBuildFeaturesAccessor
        ?: objects.newInstance(DefaultBuildFeaturesAccessor::class.java)
            .also { extraProperties.set(EXTRA_KEY, it) }

internal val Project.isConfigurationCacheEnabled
    get() = buildFeatures.isConfigurationCacheEnabled

internal val Project.isConfigurationCacheRequested
    get() = buildFeatures.isConfigurationCacheRequested

internal val Project.isProjectIsolationEnabled
    get() = buildFeatures.isProjectIsolationEnabled

internal val Project.isProjectIsolationRequested
    get() = buildFeatures.isProjectIsolationRequested
