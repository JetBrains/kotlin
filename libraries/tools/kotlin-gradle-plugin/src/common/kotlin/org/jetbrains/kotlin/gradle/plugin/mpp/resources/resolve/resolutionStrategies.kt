/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.resources.resolve

import org.gradle.api.file.FileCollection
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnostic
import org.jetbrains.kotlin.gradle.plugin.mpp.internal
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.copyAttributesTo

enum class KotlinTargetResourcesResolutionStrategy {
    VariantReselection,
    ResourcesConfiguration;

    fun resourceArchives(
        compilation: KotlinCompilation<*>,
    ): FileCollection {
        return when (this) {
            VariantReselection -> {
                // wasm targets resolve their dependencies through runtimeDependencyConfiguration
                val dependenciesConfiguration = if (compilation.target is KotlinJsIrTarget) {
                    compilation.internal.configurations.runtimeDependencyConfiguration
                        ?: return compilation.project.files().also {
                            compilation.project.reportDiagnostic(
                                KotlinToolingDiagnostics.MissingConfigurationForWasmTarget(compilation.target.name)
                            )
                        }
                } else {
                    compilation.internal.configurations.compileDependencyConfiguration
                }
                dependenciesConfiguration.incoming.artifactView {
                    it.withVariantReselection()
                    it.attributes { viewAttributes ->
                        compilation.project.configurations.getByName(
                            compilation.target.internal.resourcesElementsConfigurationName
                        ).copyAttributesTo(
                            compilation.project,
                            viewAttributes,
                        )
                    }
                    it.isLenient = true
                }.files
            }

            ResourcesConfiguration -> compilation.internal.configurations.resourcesConfiguration
        }
    }

    val propertyName: String
        get() = when (this) {
            VariantReselection -> "variantReselection"
            ResourcesConfiguration -> "resourcesConfiguration"
        }

    companion object {
        fun fromProperty(name: String): KotlinTargetResourcesResolutionStrategy? = when (name) {
            VariantReselection.propertyName -> VariantReselection
            ResourcesConfiguration.propertyName -> ResourcesConfiguration
            else -> null
        }
    }
}