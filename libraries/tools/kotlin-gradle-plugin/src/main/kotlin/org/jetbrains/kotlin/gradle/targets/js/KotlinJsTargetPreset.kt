/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("PackageDirectoryMismatch")

// Old package for compatibility
package org.jetbrains.kotlin.gradle.plugin.mpp

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinJsTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrSingleTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetConfigurator
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTargetPreset

open class KotlinJsTargetPreset(
    project: Project,
    kotlinPluginVersion: String,
    val irPreset: KotlinJsIrTargetPreset?
) : KotlinOnlyTargetPreset<KotlinJsTarget, KotlinJsCompilation>(
    project,
    kotlinPluginVersion
) {
    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun instantiateTarget(): KotlinJsTarget {

        val irTarget = irPreset?.createTarget("JsIr")

        return project.objects.newInstance(
            KotlinJsTarget::class.java,
            project,
            platformType
        ).apply {
            this.irTarget = irTarget
        }
    }

    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? =
        if (irPreset == null) {
            super.provideTargetDisambiguationClassifier(target)
        } else {
            LEGACY_DISAMBIGUATION_CLASSIFIER +
                    super.provideTargetDisambiguationClassifier(target)
        }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion,
        irPreset?.let { KotlinJsIrTargetConfigurator(kotlinPluginVersion) }
    )

    override fun getName(): String = PRESET_NAME

    override fun createCompilationFactory(forTarget: KotlinOnlyTarget<KotlinJsCompilation>): KotlinJsCompilationFactory {
        forTarget as KotlinJsTarget
        return KotlinJsCompilationFactory(project, forTarget, irPreset?.let { forTarget.irTarget })
    }

    companion object {
        const val PRESET_NAME = "js"
    }
}

class KotlinJsSingleTargetPreset(
    project: Project,
    kotlinPluginVersion: String,
    irPreset: KotlinJsIrSingleTargetPreset?
) : KotlinJsTargetPreset(
    project,
    kotlinPluginVersion,
    irPreset
) {

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsCompilation>): String? =
        irPreset?.let { LEGACY_DISAMBIGUATION_CLASSIFIER }

    override fun createKotlinTargetConfigurator() = KotlinJsTargetConfigurator(
        kotlinPluginVersion,
        irPreset?.let { KotlinJsIrTargetConfigurator(kotlinPluginVersion) }
    )
}

const val LEGACY_DISAMBIGUATION_CLASSIFIER = "legacy"