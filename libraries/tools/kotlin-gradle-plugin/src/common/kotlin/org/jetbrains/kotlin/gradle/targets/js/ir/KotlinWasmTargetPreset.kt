/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithWasmPresetFunctions.Companion.DEFAULT_WASM_JS_NAME
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithWasmPresetFunctions.Companion.DEFAULT_WASM_SPEC_NAME
import org.jetbrains.kotlin.gradle.dsl.KotlinTargetContainerWithWasmPresetFunctions.Companion.DEFAULT_WASM_WASI_NAME
import org.jetbrains.kotlin.gradle.plugin.AbstractKotlinTargetConfigurator
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget.Companion.buildNpmProjectName
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

internal class KotlinWasmTargetPreset(
    project: Project,
    private val targetType: KotlinWasmTargetType,
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(project) {
    override val platformType: KotlinPlatformType = KotlinPlatformType.wasm

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        val irTarget = project.objects.KotlinJsIrTarget(project, KotlinPlatformType.wasm, true)
        irTarget.outputModuleName.convention(
            buildNpmProjectName(
                project,
                name,
                when (targetType) {
                    KotlinWasmTargetType.JS -> DEFAULT_WASM_JS_NAME
                    KotlinWasmTargetType.WASI -> DEFAULT_WASM_WASI_NAME
                    KotlinWasmTargetType.SPEC -> DEFAULT_WASM_SPEC_NAME
                }
            )
        )
        irTarget.wasmTargetType = targetType

        return irTarget
    }

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override val name: String = WASM_PRESET_NAME + targetType.name.toLowerCaseAsciiOnly().capitalizeAsciiOnly()

    override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(forTarget)

    companion object {
        internal const val WASM_PRESET_NAME = "wasm"
    }
}
