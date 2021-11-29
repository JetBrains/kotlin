/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinFragmentModuleCapabilityConfigurator.setModuleCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

interface KotlinRuntimeElementsConfigurationFactory : KotlinFragmentConfigurationInstantiator

object DefaultKotlinRuntimeElementsConfigurationInstantiator : KotlinRuntimeElementsConfigurationFactory {
    override fun create(
        module: KotlinGradleModule,
        names: FragmentNameDisambiguation,
        dependencies: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("runtimeElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false

            attributes.attribute(Category.CATEGORY_ATTRIBUTE, module.project.objects.named(Category::class.java, Category.LIBRARY))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, module.project.objects.named(Bundling::class.java, Bundling.EXTERNAL))

            extendsFrom(dependencies.transitiveApiConfiguration)
            extendsFrom(dependencies.transitiveImplementationConfiguration)
            module.ifMadePublic {
                isCanBeConsumed = true
                setModuleCapability(this, module)
            }
        }
    }
}

val DefaultKotlinRuntimeElementsConfigurator = KotlinConfigurationsConfigurator(
    KotlinFragmentPlatformAttributesConfigurator,
    KotlinFragmentProducerRuntimeUsageAttributesConfigurator
)
