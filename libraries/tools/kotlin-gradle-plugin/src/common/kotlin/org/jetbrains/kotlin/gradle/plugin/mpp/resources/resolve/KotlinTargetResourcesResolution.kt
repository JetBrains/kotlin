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
import org.jetbrains.kotlin.gradle.plugin.mpp.publishing.configureResourcesPublicationAttributes
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal object KotlinTargetResourcesResolution {
    fun resourceArchives(
        compilation: KotlinCompilation<*>,
    ): FileCollection {
        val dependenciesConfiguration = if (compilation.target is KotlinJsIrTarget) {
            compilation.internal.configurations.runtimeDependencyConfiguration
                ?: return compilation.project.files().also {
                    compilation.project.reportDiagnostic(
                        KotlinToolingDiagnostics.MissingRuntimeDependencyConfigurationForWasmTarget(compilation.target.name)
                    )
                }
        } else {
            compilation.internal.configurations.compileDependencyConfiguration
        }
        return dependenciesConfiguration.incoming.artifactView {
            it.withVariantReselection()
            it.attributes { viewAttributes ->
                viewAttributes.configureResourcesPublicationAttributes(compilation.target)
            }
            it.isLenient = true
        }.files
    }
}