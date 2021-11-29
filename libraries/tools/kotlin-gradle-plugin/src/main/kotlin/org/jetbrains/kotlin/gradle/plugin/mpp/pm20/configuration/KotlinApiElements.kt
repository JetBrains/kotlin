/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.pm20.configuration

import org.gradle.api.artifacts.Configuration
import org.gradle.api.attributes.Bundling
import org.gradle.api.attributes.Category
import org.gradle.jvm.tasks.Jar
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.FragmentNameDisambiguation
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.disambiguateName
import org.jetbrains.kotlin.gradle.tasks.locateOrRegisterTask
import org.jetbrains.kotlin.gradle.utils.dashSeparatedName

interface KotlinApiElementsConfigurationInstantiator : KotlinFragmentConfigurationInstantiator

object DefaultKotlinApiElementsConfigurationInstantiator : KotlinApiElementsConfigurationInstantiator {
    override fun create(
        module: KotlinGradleModule,
        names: FragmentNameDisambiguation,
        dependencies: KotlinDependencyConfigurations
    ): Configuration {
        return module.project.configurations.maybeCreate(names.disambiguateName("apiElements")).apply {
            isCanBeResolved = false
            isCanBeConsumed = false
            extendsFrom(dependencies.transitiveApiConfiguration)
            module.ifMadePublic { isCanBeConsumed = true }

            attributes.attribute(Category.CATEGORY_ATTRIBUTE, module.project.objects.named(Category::class.java, Category.LIBRARY))
            attributes.attribute(Bundling.BUNDLING_ATTRIBUTE, module.project.objects.named(Bundling::class.java, Bundling.EXTERNAL))
        }
    }
}

val DefaultKotlinApiElementsConfigurator = KotlinConfigurationsConfigurator(
    KotlinFragmentPlatformAttributesConfigurator,
    KotlinFragmentModuleCapabilityConfigurator,
    KotlinFragmentProducerApiUsageAttributesConfigurator
)

object KotlinCompilationOutputsJarArtifactConfigurator : KotlinFragmentConfigurationsConfigurator<KotlinGradleVariant> {
    override fun configure(fragment: KotlinGradleVariant, configuration: Configuration) {
        val project = fragment.project
        val module = fragment.containingModule
        val jarTaskName = fragment.disambiguateName("jar")
        val jarTask = project.locateOrRegisterTask<Jar>(jarTaskName) {
            it.from(fragment.compilationOutputs.allOutputs)
            it.archiveClassifier.set(dashSeparatedName(fragment.name, module.moduleClassifier))
        }
        project.artifacts.add(configuration.name, jarTask)
    }
}
