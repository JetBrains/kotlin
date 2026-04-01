/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithJsPresetFunctions.Companion.DEFAULT_JS_NAME
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinOnlyTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinJsBinaryTypeMetrics
import org.jetbrains.kotlin.gradle.plugin.statistics.KotlinJsIrTargetMetrics
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget.Companion.buildNpmProjectName

internal open class KotlinJsIrTargetPreset(
    project: Project,
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(
    project
) {
    protected open val isMpp: Boolean
        get() = true

    override val platformType: KotlinPlatformType = KotlinPlatformType.js

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        return project.objects.KotlinJsIrTarget(project, platformType, isMpp).apply {
            this.outputModuleName.convention(buildNpmProjectName(project, name, DEFAULT_JS_NAME))
            KotlinJsIrTargetMetrics.collectMetrics(
                isBrowserConfigured = isBrowserConfigured,
                isNodejsConfigured = isNodejsConfigured,
                project
            )
            KotlinJsBinaryTypeMetrics.collectMetrics(this, project)
        }
    }

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override val name: String = JS_PRESET_NAME

    //TODO[Ilya Goncharov] remove public morozov
    override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget,
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(forTarget)

    companion object {
        val JS_PRESET_NAME = "js"
    }
}

internal class KotlinJsIrSingleTargetPreset(
    project: Project,
) : KotlinJsIrTargetPreset(
    project
) {
    override val isMpp: Boolean
        get() = false

    // In a Kotlin/JS single-platform project, we don't need any disambiguation suffixes or prefixes in the names:
    override fun provideTargetDisambiguationClassifier(target: KotlinOnlyTarget<KotlinJsIrCompilation>): String? {
        return null
    }

    override fun createKotlinTargetConfigurator(): KotlinOnlyTargetConfigurator<KotlinJsIrCompilation, KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()
}
