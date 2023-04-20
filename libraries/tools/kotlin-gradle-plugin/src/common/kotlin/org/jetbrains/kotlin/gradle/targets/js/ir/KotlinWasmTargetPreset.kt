/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.PropertiesProvider
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset
import org.jetbrains.kotlin.gradle.utils.runProjectConfigurationHealthCheckWhenEvaluated

class KotlinWasmTargetPreset(
    project: Project,
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(project) {
    override val platformType: KotlinPlatformType = KotlinPlatformType.wasm

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        if (!PropertiesProvider(project).wasmStabilityNoWarn) {
            project.logger.warn("New 'wasm' target is Work-in-Progress and is subject to change without notice.")
        }

        val irTarget = project.objects.newInstance(KotlinJsIrTarget::class.java, project, KotlinPlatformType.wasm, false)
        irTarget.isMpp = true

        return irTarget
    }

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override fun getName(): String = WASM_PRESET_NAME

    public override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(forTarget)

    companion object {
        private const val WASM_PRESET_NAME = "wasm"
    }
}
