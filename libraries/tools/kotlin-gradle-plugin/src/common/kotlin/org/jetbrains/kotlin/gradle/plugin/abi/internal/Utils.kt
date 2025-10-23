/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi.internal

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.jetbrains.kotlin.abi.tools.KlibTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.createResolvable

private const val ABI_TOOLS_DEPENDENCY_CONFIGURATION = "kotlinInternalAbiValidation"

internal const val ANDROID_RELEASE_BUILD_TYPE = "release"

/**
 * Converts a [KotlinTarget] to a [KlibTarget].
 */
internal fun KotlinTarget.toKlibTarget(): KlibTarget {
    if (this is KotlinNativeTarget) {
        return KlibTarget.fromKonanTargetName(konanTarget.name).configureName(targetName)
    }
    val name = when (platformType) {
        KotlinPlatformType.js -> "js"
        KotlinPlatformType.wasm -> when ((this as KotlinJsIrTarget).wasmTargetType) {
            KotlinWasmTargetType.WASI -> "wasmWasi"
            KotlinWasmTargetType.JS -> "wasmJs"
            else -> throw IllegalStateException("Unreachable")
        }
        else -> throw IllegalArgumentException("Unsupported platform type: $platformType")
    }
    return KlibTarget(name, targetName)
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


internal fun Project.prepareAbiClasspath(): Configuration {
    val version = getKotlinPluginVersion()

    return configurations.createResolvable(ABI_TOOLS_DEPENDENCY_CONFIGURATION)
        .also {
            @Suppress("DEPRECATION")
            it.isVisible = false
            it.defaultDependencies { dependencies ->
                dependencies.add(
                    project.dependencies.create("org.jetbrains.kotlin:abi-tools:$version")
                )
            }
        }
}

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