/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.isHostSpecificKonanTargetsSet
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinHostSpecificMetadataElementsConfigurationInstantiator : KotlinFragmentConfigurationInstantiator

fun DefaultKotlinHostSpecificMetadataElementsConfigurationInstantiator(
    konanTarget: KonanTarget
): KotlinHostSpecificMetadataElementsConfigurationInstantiator? = if (isHostSpecificKonanTargetsSet(setOf(konanTarget)))
    DefaultKotlinHostSpecificMetadataElementsConfigurationInstantiator else null

private object DefaultKotlinHostSpecificMetadataElementsConfigurationInstantiator :
    KotlinHostSpecificMetadataElementsConfigurationInstantiator {
    override fun create(
        module: KotlinGradleModule, names: FragmentNameDisambiguation, dependencies: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("hostSpecificMetadataElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
        }
    }
}

val DefaultHostSpecificMetadataElementsConfigurator = KotlinConfigurationsConfigurator(
    KotlinFragmentPlatformAttributesConfigurator,
    KonanTargetAttributesConfigurator,
    KotlinFragmentMetadataUsageAttributeConfigurator
)
