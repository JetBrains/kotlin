/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.configuration.BuildFeatures
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory
import javax.inject.Inject

/**
 * KGP was using `StartParametersInternal.isConfigurationCache` to find out whether the build requests configuration cache.
 *
 * That property became deprecated since Gradle 7.1 and is scheduled to be removed soon.
 *
 * As a replacement, Gradle provides [org.gradle.StartParameter.isConfigurationCacheRequested] since Gradle 7.6, which is deprecated
 * in 8.5 release.
 *
 * As a replacement, Gradle provides [BuildFeatures.getConfigurationCache] service since Gradle 8.5.
 */
internal interface ConfigurationCacheStartParameterAccessor {
    val isConfigurationCacheEnabled: Boolean
    val isConfigurationCacheRequested: Boolean

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(project: Project): ConfigurationCacheStartParameterAccessor
    }
}

internal class DefaultConfigurationCacheStartParameterAccessorVariantFactory :
    ConfigurationCacheStartParameterAccessor.Factory {
    override fun getInstance(project: Project): ConfigurationCacheStartParameterAccessor = project
        .objects
        .newInstance(DefaultConfigurationCacheStartParameterAccessor::class.java)
}

internal abstract class DefaultConfigurationCacheStartParameterAccessor @Inject constructor(
    buildFeatures: BuildFeatures,
) : ConfigurationCacheStartParameterAccessor {
    override val isConfigurationCacheEnabled: Boolean = buildFeatures.configurationCache.active.orElse(false).get()
    override val isConfigurationCacheRequested: Boolean = buildFeatures.configurationCache.requested.orElse(false).get()
}

internal val Project.isConfigurationCacheEnabled
    get() = variantImplementationFactory<ConfigurationCacheStartParameterAccessor.Factory>()
        .getInstance(this)
        .isConfigurationCacheEnabled

internal val Project.isConfigurationCacheRequested
    get() = variantImplementationFactory<ConfigurationCacheStartParameterAccessor.Factory>()
        .getInstance(this)
        .isConfigurationCacheRequested
