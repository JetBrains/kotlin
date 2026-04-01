/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetId
import org.jetbrains.kotlin.buildtools.api.abi.KlibTargetType
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget

internal const val ANDROID_RELEASE_BUILD_TYPE = "release"

/**
 * Converts a [KotlinTarget] to a [KlibTargetId].
 */
internal fun KotlinTarget.toKlibTarget(): KlibTargetId {
    if (this is KotlinNativeTarget) {
        val targetType = KlibTargetType.fromKonanTargetName(konanTarget.name)
        return KlibTargetId(targetType, targetName)
    }
    val targetType = when (platformType) {
        KotlinPlatformType.js -> KlibTargetType.JS
        KotlinPlatformType.wasm -> when ((this as KotlinJsIrTarget).wasmTargetType) {
            KotlinWasmTargetType.WASI -> KlibTargetType.WASM_WASI
            KotlinWasmTargetType.JS -> KlibTargetType.WASM_JS
            else -> throw IllegalStateException("Unreachable")
        }
        else -> throw IllegalArgumentException("Unsupported platform type: $platformType")
    }
    return KlibTargetId(targetType, targetName)
}

/**
 * Check the specified target has a klib file as its output artifact.
 */
internal val KotlinTarget.emitsKlib: Boolean
    get() {
        val platformType = this.platformType
        return platformType == KotlinPlatformType.native ||
                platformType == KotlinPlatformType.wasm ||
                platformType == KotlinPlatformType.js
    }

@Suppress("TYPEALIAS_EXPANSION_DEPRECATION")
internal val org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidBaseVariant.isTestVariant: Boolean
    get() = this is org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidTestVariant || this is org.jetbrains.kotlin.gradle.utils.DeprecatedAndroidUnitTestVariant

/**
 * Executes a given [action] against the compilation with the name [compilationName].
 */
internal inline fun <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.withCompilationIfExists(
    compilationName: String,
    crossinline action: T.() -> Unit,
) {
    if (names.contains(compilationName)) {
        named(compilationName).configure { compilation ->
            compilation.action()
        }
    }
}