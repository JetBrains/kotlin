/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.internal

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.variantImplementationFactory

/**
 * Provides unified safe way to read environmental properties via [Provider] at configuration time in terms of Gradle configuration cache feature
 * Gradle 6.5 - 7.3 was requiring to explicitly mark such reads to be able to invalidate configuration cache entries on the value change.
 * Gradle 7.4+ is able to automatically detect such reads without explicit declaration, thus method to declare a read was deprecated
 */
internal interface ConfigurationTimePropertiesAccessor {
    fun <T> Provider<T>.usedAtConfigurationTime(): Provider<T>

    interface ConfigurationTimePropertiesAccessorVariantFactory : VariantImplementationFactories.VariantImplementationFactory {
        fun getInstance(): ConfigurationTimePropertiesAccessor
    }
}

internal class DefaultConfigurationTimePropertiesAccessorVariantFactory :
    ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory {
    override fun getInstance(): ConfigurationTimePropertiesAccessor = DefaultConfigurationTimePropertiesAccessor()
}

/**
 * No-op implementation for fresh Gradle versions since no declaration is required
 */
internal class DefaultConfigurationTimePropertiesAccessor : ConfigurationTimePropertiesAccessor {
    override fun <T> Provider<T>.usedAtConfigurationTime(): Provider<T> = this
}

internal val Project.configurationTimePropertiesAccessor
    get() = variantImplementationFactory<ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory>()
        .getInstance()

internal fun <T> Provider<T>.usedAtConfigurationTime(accessor: ConfigurationTimePropertiesAccessor) = with(accessor) {
    usedAtConfigurationTime()
}