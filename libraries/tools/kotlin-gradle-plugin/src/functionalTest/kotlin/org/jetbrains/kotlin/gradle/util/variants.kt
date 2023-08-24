/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.util

import org.gradle.api.invocation.Gradle
import org.jetbrains.kotlin.gradle.plugin.VariantImplementationFactoriesConfigurator
import org.jetbrains.kotlin.gradle.plugin.internal.*
import org.jetbrains.kotlin.gradle.plugin.internal.ConfigurationTimePropertiesAccessor
import org.jetbrains.kotlin.gradle.plugin.internal.DefaultConfigurationTimePropertiesAccessorVariantFactory
import org.jetbrains.kotlin.gradle.plugin.internal.IdeaSyncDetector


/**
 * Configures some default factories that are usually automatically registered in
 * [org.jetbrains.kotlin.gradle.plugin.DefaultKotlinBasePlugin.apply]
 *
 * This function can be used in some minimal tests that do not apply the full KGP plugin but still touch
 * some parts of its code
 */
fun Gradle.registerMinimalVariantImplementationFactoriesForTests() {
    VariantImplementationFactoriesConfigurator.get(gradle).putIfAbsent(
        ConfigurationTimePropertiesAccessor.ConfigurationTimePropertiesAccessorVariantFactory::class,
        DefaultConfigurationTimePropertiesAccessorVariantFactory()
    )

    // Diagnostics need to know if we're in IDEA sync in order to decide whether the stacktrace
    // should be reported
    VariantImplementationFactoriesConfigurator.get(gradle).putIfAbsent(
        IdeaSyncDetector.IdeaSyncDetectorVariantFactory::class,
        DefaultIdeaSyncDetectorVariantFactory()
    )
}
