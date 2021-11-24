/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.gradle.plugin.mpp.isHostSpecificKonanTargetsSet
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.jetbrains.kotlin.konan.target.KonanTarget

interface KotlinNativeHostSpecificMetadataElementsConfigurationFactory : KotlinFragmentConfigurationFactory

fun DefaultKotlinNativeHostSpecificMetadataElementsConfigurationFactory(
    konanTarget: KonanTarget
): KotlinNativeHostSpecificMetadataElementsConfigurationFactory? = if (isHostSpecificKonanTargetsSet(setOf(konanTarget)))
    DefaultKotlinNativeHostSpecificMetadataElementsConfigurationFactory else null

private object DefaultKotlinNativeHostSpecificMetadataElementsConfigurationFactory :
    KotlinNativeHostSpecificMetadataElementsConfigurationFactory {
    override fun create(
        module: KotlinGradleModule, names: FragmentNameDisambiguation, dependencies: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("hostSpecificMetadataElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
        }
    }
}

