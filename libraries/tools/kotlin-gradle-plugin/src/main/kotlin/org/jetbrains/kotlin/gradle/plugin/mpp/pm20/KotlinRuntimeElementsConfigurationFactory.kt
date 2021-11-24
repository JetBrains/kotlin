/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KotlinModuleCapabilitySetup.setModuleCapability
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation

interface KotlinRuntimeElementsConfigurationFactory : KotlinFragmentConfigurationFactory

object DefaultKotlinRuntimeElementsConfigurationFactory : KotlinRuntimeElementsConfigurationFactory {
    override fun create(
        module: KotlinGradleModule,
        names: FragmentNameDisambiguation,
        dependencyConfigurations: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("runtimeElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false

            attributes.attribute(Category.CATEGORY_ATTRIBUTE, module.project.objects.named(Category::class.java, Category.LIBRARY))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, module.project.objects.named(Bundling::class.java, Bundling.EXTERNAL))

            extendsFrom(dependencyConfigurations.transitiveApiConfiguration)
            extendsFrom(dependencyConfigurations.transitiveImplementationConfiguration)
            module.ifMadePublic {
                isCanBeConsumed = true
                setModuleCapability(this, module)
            }
        }
    }
}
