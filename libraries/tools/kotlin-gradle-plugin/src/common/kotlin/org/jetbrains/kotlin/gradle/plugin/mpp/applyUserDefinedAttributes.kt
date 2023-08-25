/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.utils.copyAttributes


/**
 * The attributes attached to the targets and compilations need to be propagated to the relevant Gradle configurations:
 * 1. Output configurations of each target need the corresponding compilation's attributes (and, indirectly, the target's attributes)
 * 2. Resolvable configurations of each compilation need the compilation's attributes
 */
internal fun applyUserDefinedAttributes(target: InternalKotlinTarget) {
    target.project.launchInStage(AfterEvaluateBuildscript) {
        target.kotlinComponents.flatMap { it.internal.usages }.forEach { usage ->
            val dependencyConfiguration = target.project.configurations.findByName(usage.dependencyConfigurationName) ?: return@forEach
            copyAttributes(usage.compilation.attributes, dependencyConfiguration.attributes)
        }

        target.compilations.all { compilation ->
            val compilationAttributes = compilation.attributes

            compilation.allOwnedConfigurationsNames
                .mapNotNull { configurationName -> project.configurations.findByName(configurationName) }
                .forEach { configuration -> copyAttributes(compilationAttributes, configuration.attributes) }
        }

        // Copy to host-specific metadata elements configurations
        if (target is KotlinNativeTarget) {
            val hostSpecificMetadataElements = project.configurations.findByName(target.hostSpecificMetadataElementsConfigurationName)
            if (hostSpecificMetadataElements != null) {
                copyAttributes(from = target.attributes, to = hostSpecificMetadataElements.attributes)
            }
        }
    }
}

private val KotlinCompilation<*>.allOwnedConfigurationsNames
    get(): List<String> {
        val defaultConfigurations = listOfNotNull(
            apiConfigurationName,
            implementationConfigurationName,
            compileOnlyConfigurationName,
            runtimeOnlyConfigurationName,
            compileDependencyConfigurationName,
            runtimeDependencyConfigurationName,
            internal.configurations.hostSpecificMetadataConfiguration?.name,
        )

        val implementationSpecificConfigurations = when (this) {
            is KotlinJvmAndroidCompilation -> listOfNotNull(
                "${androidVariant.name}ApiElements",
                "${androidVariant.name}RuntimeElements",
                androidVariant.compileConfiguration.name,
                androidVariant.runtimeConfiguration.name
            )
            is KotlinJsCompilation -> listOfNotNull(npmAggregatedConfigurationName, publicPackageJsonConfigurationName)
            else -> emptyList()
        }

        return defaultConfigurations + implementationSpecificConfigurations
    }