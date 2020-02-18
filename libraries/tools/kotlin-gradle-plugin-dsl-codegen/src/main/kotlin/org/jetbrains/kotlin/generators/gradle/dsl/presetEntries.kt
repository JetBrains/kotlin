/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.generators.gradle.dsl

import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Presets
import org.jetbrains.kotlin.generators.gradle.dsl.NativeFQNames.Targets
import org.jetbrains.kotlin.gradle.plugin.JsCompilerType
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.presetName

internal open class KotlinPresetEntry(
    val presetName: String,
    val presetType: TypeName,
    val targetType: TypeName
) {
    open fun typeNames(): Set<TypeName> = setOf(presetType, targetType)

    open fun generatePresetFunctions(
        getPresetsExpression: String,
        configureOrCreateFunctionName: String
    ): String {
        val presetName = presetName
        return """
    fun $presetName(
        name: String = "$presetName",
        configure: ${targetType.renderShort()}.() -> Unit = { }
    ): ${targetType.renderShort()} =
        $configureOrCreateFunctionName(
            name,
            $getPresetsExpression.getByName("$presetName") as ${presetType.renderShort()},
            configure
        )

    fun $presetName() = $presetName("$presetName") { }
    fun $presetName(name: String) = $presetName(name) { }
    fun $presetName(name: String, configure: Closure<*>) = $presetName(name) { ConfigureUtil.configure(configure, this) }
    fun $presetName(configure: Closure<*>) = $presetName { ConfigureUtil.configure(configure, this) }
""".trimIndent()
    }
}

internal class KotlinJsPresetEntry(
    presetName: String,
    presetType: TypeName,
    targetType: TypeName
) : KotlinPresetEntry(
    presetName,
    presetType,
    targetType
) {
    override fun typeNames(): Set<TypeName> {
        return setOf(
            typeName(JsCompilerType::class.qualifiedName!!),
            typeName("org.jetbrains.kotlin.gradle.utils.lowerCamelCaseName"),
            presetType,
            targetType
        )
    }

    override fun generatePresetFunctions(
        getPresetsExpression: String,
        configureOrCreateFunctionName: String
    ): String {
        val presetName = presetName
        return """
    fun $presetName(
        name: String = "$presetName",
        compiler: JsCompilerType = defaultJsCompilerType,
        configure: ${targetType.renderShort()}.() -> Unit = { }
    ): ${targetType.renderShort()} =
        $configureOrCreateFunctionName(
            lowerCamelCaseName(name, if (compiler == JsCompilerType.both) JsCompilerType.legacy.name else null),
            $getPresetsExpression.getByName(
                lowerCamelCaseName(
                    "$presetName",
                    if (compiler == JsCompilerType.legacy) null else compiler.name
                )
            ) as ${presetType.renderShort()},
            configure
        )
    
    fun $presetName(
        name: String = "$presetName",
        configure: ${targetType.renderShort()}.() -> Unit = { }
    ) = $presetName(name = "$presetName", compiler = defaultJsCompilerType, configure = configure)
    
    fun $presetName(
        compiler: JsCompilerType,
        configure: ${targetType.renderShort()}.() -> Unit = { }
    ) = $presetName(name = "$presetName", compiler = compiler, configure = configure)

    fun $presetName() = $presetName(name = "$presetName") { }
    fun $presetName(name: String) = $presetName(name = name) { }
    fun $presetName(name: String, configure: Closure<*>) = $presetName(name = name) { ConfigureUtil.configure(configure, this) }
    fun $presetName(compiler: JsCompilerType, configure: Closure<*>) = $presetName(compiler = compiler) { ConfigureUtil.configure(configure, this) }
    fun $presetName(configure: Closure<*>) = $presetName { ConfigureUtil.configure(configure, this) }
""".trimIndent()
    }
}

internal const val MPP_PACKAGE = "org.jetbrains.kotlin.gradle.plugin.mpp"

internal object NativeFQNames {
    object Targets {
        const val base = "$MPP_PACKAGE.KotlinNativeTarget"
        const val withHostTests = "$MPP_PACKAGE.KotlinNativeTargetWithHostTests"
        const val withSimulatorTests = "$MPP_PACKAGE.KotlinNativeTargetWithSimulatorTests"
    }

    object Presets {
        const val simple = "$MPP_PACKAGE.KotlinNativeTargetPreset"
        const val withHostTests = "$MPP_PACKAGE.KotlinNativeTargetWithHostTestsPreset"
        const val withSimulatorTests = "$MPP_PACKAGE.KotlinNativeTargetWithSimulatorTestsPreset"
    }
}

internal val jvmPresetEntry = KotlinPresetEntry(
    "jvm",
    typeName("$MPP_PACKAGE.KotlinJvmTargetPreset"),
    typeName("org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget")
)

internal val jsPresetEntry = KotlinJsPresetEntry(
    "js",
    // need for commonization KotlinJsTargetPreset and KotlinJsIrTargetPreset
    typeName("org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset", "org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl"),
    typeName("org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl")
)

internal val androidPresetEntry = KotlinPresetEntry(
    "android",
    typeName("$MPP_PACKAGE.KotlinAndroidTargetPreset"),
    typeName("$MPP_PACKAGE.KotlinAndroidTarget")
)

// Note: modifying these sets should also be reflected in the MPP plugin code, see 'setupDefaultPresets'
private val nativeTargetsWithHostTests = setOf(KonanTarget.LINUX_X64, KonanTarget.MACOS_X64, KonanTarget.MINGW_X64)
private val nativeTargetsWithSimulatorTests = setOf(KonanTarget.IOS_X64, KonanTarget.WATCHOS_X86, KonanTarget.TVOS_X64)
private val disabledNativeTargets = setOf(KonanTarget.WATCHOS_X64)

internal val nativePresetEntries = HostManager().targets
    .filter { (_, target) -> target !in disabledNativeTargets }
    .map { (_, target) ->

        val (presetType, targetType) = when (target) {
            in nativeTargetsWithHostTests ->
                Presets.withHostTests to Targets.withHostTests
            in nativeTargetsWithSimulatorTests ->
                Presets.withSimulatorTests to Targets.withSimulatorTests
            else ->
                Presets.simple to Targets.base
        }

        KotlinPresetEntry(target.presetName, typeName(presetType), typeName(targetType))
    }

internal val allPresetEntries = listOf(
    jvmPresetEntry,
    jsPresetEntry,
    androidPresetEntry
) + nativePresetEntries