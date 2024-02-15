/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * KGP was using `StartParametersInternal.isConfigurationCache` to find out whether the build requests configuration cache.
 * That property became deprecated since Gradle 7.1 and is scheduled to be removed soon.
 * As a replacement, Gradle provides [org.gradle.StartParameter.isConfigurationCacheRequested] since Gradle 7.6
 */
internal interface ConfigurationCacheStartParameterAccessor {
    val isConfigurationCacheRequested: Boolean

    interface Factory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(gradle: Gradle): ConfigurationCacheStartParameterAccessor
    }
}

internal class DefaultConfigurationCacheStartParameterAccessorVariantFactory :
    ConfigurationCacheStartParameterAccessor.Factory {
    override fun getInstance(gradle: Gradle) = DefaultConfigurationCacheStartParameterAccessor(gradle)
}

internal class DefaultConfigurationCacheStartParameterAccessor(
    private val gradle: Gradle,
) : ConfigurationCacheStartParameterAccessor {
    override val isConfigurationCacheRequested: Boolean by lazy {
        @Suppress("DEPRECATION") // TODO: will be fixed separately via KT-64355
        gradle.startParameter.isConfigurationCacheRequested
    }
}

internal val Project.isConfigurationCacheRequested
    get() = variantImplementationFactory<ConfigurationCacheStartParameterAccessor.Factory>()
        .getInstance(gradle)
        .isConfigurationCacheRequested
