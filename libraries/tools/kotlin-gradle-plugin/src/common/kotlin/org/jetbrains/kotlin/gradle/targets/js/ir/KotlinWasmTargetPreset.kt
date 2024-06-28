/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.ir

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.DeprecatedTargetPresetApi
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.diagnostics.KotlinToolingDiagnostics
import org.jetbrains.kotlin.gradle.plugin.diagnostics.reportDiagnosticOncePerBuild
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinCompilationFactory
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinOnlyTargetPreset
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.util.capitalizeDecapitalize.capitalizeAsciiOnly
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

@DeprecatedTargetPresetApi
class KotlinWasmTargetPreset(
    project: Project,
    private val targetType: KotlinWasmTargetType
) : KotlinOnlyTargetPreset<KotlinJsIrTarget, KotlinJsIrCompilation>(project) {
    override val platformType: KotlinPlatformType = KotlinPlatformType.wasm

    override fun instantiateTarget(name: String): KotlinJsIrTarget {
        val irTarget = project.objects.newInstance(KotlinJsIrTarget::class.java, project, KotlinPlatformType.wasm)
        irTarget.isMpp = true
        irTarget.wasmTargetType = targetType

        return irTarget
    }

    override fun createKotlinTargetConfigurator(): AbstractKotlinTargetConfigurator<KotlinJsIrTarget> =
        KotlinJsIrTargetConfigurator()

    override fun getName(): String = WASM_PRESET_NAME + targetType.name.toLowerCaseAsciiOnly().capitalizeAsciiOnly()

    public override fun createCompilationFactory(
        forTarget: KotlinJsIrTarget
    ): KotlinCompilationFactory<KotlinJsIrCompilation> =
        KotlinJsIrCompilationFactory(forTarget)

    companion object {
        internal const val WASM_PRESET_NAME = "wasm"
    }
}
