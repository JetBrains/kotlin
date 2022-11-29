/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactories
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.DefaultConfigurationTimePropertiesAccessorVariantFactory

/**
 * [ConfigurationTimePropertiesAccessor] default factory is automatically registered in [org.jetbrains.kotlin.gradle.plugin.DefaultKotlinBasePlugin.apply]
 * This helper function is for simple tests that are testing granular logic without applying Kotlin plugin
 */
fun Gradle.registerConfigurationTimePropertiesAccessorForTests() {
    VariantImplementationFactories.get(gradle).putIfAbsent(
        ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory::class,
        DefaultConfigurationTimePropertiesAccessorVariantFactory()
    )
}
