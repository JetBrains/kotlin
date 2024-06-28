/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp

import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.AfterEvaluateBuildscript
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.await
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo
import org.jetbrains.kotlin.gradle.utils.forAllTargets


/**
 * The attributes attached to the targets and compilations need to be propagated to the relevant Gradle configurations:
 * 1. Output configurations of each target need the corresponding compilation's attributes (and, indirectly, the target's attributes)
 * 2. Resolvable configurations of each compilation need the compilation's attributes
 */
internal val UserDefinedAttributesSetupAction = KotlinProjectSetupCoroutine {
    AfterEvaluateBuildscript.await()
    kotlinExtension.forAllTargets { target ->
        target.internal.kotlinComponents.flatMap { it.internal.usages }.forEach { usage ->
            val dependencyConfiguration = target.project.configurations.findByName(usage.dependencyConfigurationName) ?: return@forEach
            usage.compilation.copyAttributesTo(
                this@KotlinProjectSetupCoroutine,
                dest = dependencyConfiguration.attributes
            )
        }

        target.compilations.all { compilation ->
            compilation.allOwnedConfigurationsNames
                .mapNotNull { configurationName -> project.configurations.findByName(configurationName) }
                .forEach { configuration ->
                    compilation.copyAttributesTo(
                        this@KotlinProjectSetupCoroutine,
                        dest = configuration
                    )
                }
        }

        // Copy to host-specific metadata elements configurations
        if (target is KotlinNativeTarget) {
            val hostSpecificMetadataElements = project.configurations.findByName(target.hostSpecificMetadataElementsConfigurationName)
            if (hostSpecificMetadataElements != null) {
                target.copyAttributesTo(
                    this@KotlinProjectSetupCoroutine,
                    dest = hostSpecificMetadataElements
                )
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
            is KotlinJsIrCompilation -> listOfNotNull(npmAggregatedConfigurationName, publicPackageJsonConfigurationName)
            else -> emptyList()
        }

        return defaultConfigurations + implementationSpecificConfigurations
    }