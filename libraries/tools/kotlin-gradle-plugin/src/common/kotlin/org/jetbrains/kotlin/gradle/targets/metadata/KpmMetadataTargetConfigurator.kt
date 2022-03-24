/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.metadata

import org.jetbrains.kotlin.gradle.dsl.KotlinCommonOptions
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformCommonOptions
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.jetbrains.kotlin.gradle.plugin.mpp.MetadataMappedCompilationDetails
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.KpmAwareTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.kpmModules
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.metadataCompilationRegistryByModuleId
import org.jetbrains.kotlin.gradle.plugin.whenEvaluated
import org.jetbrains.kotlin.project.model.utils.variantsContainingFragment

internal class KpmMetadataTargetConfigurator(private val metadataTargetConfigurator: KotlinMetadataTargetConfigurator) :
    KpmAwareTargetConfigurator<KotlinMetadataTarget>(metadataTargetConfigurator) {

    override fun configureTarget(target: KotlinMetadataTarget) {
        super.configureTarget(target)
        configureTargetWithKpm(target)
    }

    override fun configureCompilationDefaults(target: KotlinMetadataTarget) {
        super.configureCompilationDefaults(target)

        val mainCompilation = target.compilations.create(KotlinCompilation.MAIN_COMPILATION_NAME)
        defineConfigurationsForCompilation(mainCompilation)
        createLifecycleTask(mainCompilation)
        metadataTargetConfigurator
            .buildCompilationProcessor(mainCompilation)
            .run()
        mainCompilation.compileKotlinTaskProvider.configure { it.enabled = false }
    }

    private fun configureTargetWithKpm(target: KotlinMetadataTarget) {
        target.project.whenEvaluated {
            val mainModule = target.project.kpmModules.getByName(KotlinGradleModule.MAIN_MODULE_NAME)
            val metadataCompilations = target.project.metadataCompilationRegistryByModuleId.getValue(mainModule.moduleIdentifier)
            mainModule.fragments.forEach { fragment ->
                val compilationData = metadataCompilations.getForFragmentOrNull(fragment) ?: return@forEach
                if (!compilationData.isActive) return@forEach

                val compilationDetails = MetadataMappedCompilationDetails(target, compilationData)

                val isNative = compilationData is KotlinNativeFragmentMetadataCompilationData

                @Suppress("UNCHECKED_CAST")
                val compilation = when {
                    isNative -> {
                        val konanTargets =
                            mainModule.variantsContainingFragment(fragment).map { (it as KotlinNativeVariantInternal).konanTarget }
                        KotlinSharedNativeCompilation(konanTargets, compilationDetails as CompilationDetails<KotlinCommonOptions>)
                    }
                    else -> KotlinCommonCompilation(compilationDetails as CompilationDetails<KotlinMultiplatformCommonOptions>)
                }

                target.compilations.add(compilation)
            }
        }
    }
}
