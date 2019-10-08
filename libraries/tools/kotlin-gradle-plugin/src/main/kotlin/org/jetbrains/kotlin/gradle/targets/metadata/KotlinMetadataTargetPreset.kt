/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch") // Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinCompile
import org.jetbrains.kotlin.gradle.dsl.kotlinExtension
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.sources.applyLanguageSettingsToKotlinTask
import org.jetbrains.kotlin.gradle.targets.metadata.KotlinMetadataTargetConfigurator

class KotlinMetadataTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinMetadataTarget, KotlinCommonCompilation>(
    project,
    kotlinPluginVersion
) {
    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(
        forTarget: KotlinOnlyTarget<KotlinCommonCompilation>
    ): KotlinCompilationFactory<KotlinCommonCompilation> =
        KotlinCommonCompilationFactory(forTarget)

    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.common

    companion object {
        const val PRESET_NAME = "metadata"
    }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinCommonCompilation, KotlinMetadataTarget> =
        KotlinMetadataTargetConfigurator(kotlinPluginVersion)

    override fun instantiateTarget(): KotlinMetadataTarget {
        return project.objects.newInstance(KotlinMetadataTarget::class.java, project)
    }

    override fun createTarget(name: String): KotlinMetadataTarget =
        super.createTarget(name).apply {
            val mainCompilation = compilations.getByName(KotlinCompilation.MAIN_COMPILATION_NAME)
            val commonMainSourceSet = project.kotlinExtension.sourceSets.getByName(KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME)

            mainCompilation.source(commonMainSourceSet)

            project.whenEvaluated {
                // Since there's no default source set, apply language settings from commonMain:
                val compileKotlinMetadata = mainCompilation.compileKotlinTask
                applyLanguageSettingsToKotlinTask(commonMainSourceSet.languageSettings, compileKotlinMetadata)
            }
        }
}
