/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset

open class KotlinJsIrTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(
    project,
    kotlinPluginVersion
) {
    override val platformType: KotlinPlatformType
        get() = KotlinPlatformType.js

    override fun instantiateTarget(): KotlinJsIrTarget {
        return project.objects.newInstance(KotlinJsIrTarget::class.java, project, platformType)
    }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator(kotlinPluginVersion)

    override fun getName(): String = PRESET_NAME

    //TODO[Ilya Goncharov] remove public morozov
    public override fun createCompilationFactory(
        forTarget: KotlinOnlyTarget<KotlinJsIrCompilation>
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(project, forTarget)

    companion object {
        const val PRESET_NAME = "jsIr"
    }
}

class KotlinJsIrSingleTargetPreset(
    project: Project,
    kotlinPluginVersion: String
) :
    KotlinJsIrTargetPreset(project, kotlinPluginVersion) {

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsIrCompilation>): String? = null

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator(kotlinPluginVersion)
}