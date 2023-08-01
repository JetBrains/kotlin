/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.copyAttributes
import org.jetbrains.kotlin.gradle.utils.Future
import org.jetbrains.kotlin.gradle.utils.future
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasLazyProperty

private val applyUserDefinedAttributesJobExtrasKey = extrasKeyOf<Future<Unit>>("applyUserDefinedAttributes")

internal val InternalKotlinTarget.applyUserDefinedAttributesJob: Future<Unit> by extrasLazyProperty(applyUserDefinedAttributesJobExtrasKey) {
    project.future {
        KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript.await()
        // To copy the attributes to the output configurations, find those output configurations and their producing compilations
        // based on the target's components:
        val outputConfigurationsWithCompilations = kotlinComponents.filterIsInstance<KotlinVariant>().flatMap { kotlinVariant ->
            kotlinVariant.kotlinUsagesFuture.await().mapNotNull { usageContext ->
                project.configurations.findByName(usageContext.dependencyConfigurationName)?.let { configuration ->
                    configuration to usageContext.compilation
                }
            }
        }.toMutableList()

        // Add usages of android library when its variants are grouped by flavor
        outputConfigurationsWithCompilations += kotlinComponents
            .filterIsInstance<JointAndroidKotlinTargetComponent>()
            .flatMap { variant -> variant.kotlinUsagesFuture.await() }
            .mapNotNull { usage ->
                val configuration = project.configurations.findByName(usage.dependencyConfigurationName) ?: return@mapNotNull null
                configuration to usage.compilation
            }

        outputConfigurationsWithCompilations.forEach { (configuration, compilation) ->
            copyAttributes(compilation.attributes, configuration.attributes)
        }

        compilations.all { compilation ->
            val compilationAttributes = compilation.attributes

            compilation.allOwnedConfigurationsNames
                .mapNotNull { configurationName -> project.configurations.findByName(configurationName) }
                .forEach { configuration ->
                    copyAttributes(compilationAttributes, configuration.attributes)
                }
        }

        // Copy to host-specific metadata elements configurations
        if (this@extrasLazyProperty is KotlinNativeTarget) {
            val hostSpecificMetadataElements = project.configurations.findByName(hostSpecificMetadataElementsConfigurationName)
            if (hostSpecificMetadataElements != null) {
                copyAttributes(from = attributes, to = hostSpecificMetadataElements.attributes)
            }
        }
    }
}

/**
 * The attributes attached to the targets and compilations need to be propagated to the relevant Gradle configurations:
 * 1. Output configurations of each target need the corresponding compilation's attributes (and, indirectly, the target's attributes)
 * 2. Resolvable configurations of each compilation need the compilation's attributes
 */
internal fun applyUserDefinedAttributes(target: InternalKotlinTarget) {
    target.applyUserDefinedAttributesJob // trigger the job
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