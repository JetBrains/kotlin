/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.abi

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.SourceSet
import org.jetbrains.kotlin.abi.tools.api.v2.KlibTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.utils.createResolvable

private const val ABI_TOOLS_DEPENDENCY_CONFIGURATION = "kotlinInternalAbiValidation"

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
            it.isVisible = false
            it.defaultDependencies { dependencies ->
                dependencies.add(
                    project.dependencies.create("org.jetbrains.kotlin:abi-tools:$version")
                )
            }
        }
}

/**
 * Executes a given [action] against the compilation with the name [SourceSet.MAIN_SOURCE_SET_NAME].
 */
internal inline fun <T : KotlinCompilation<*>> NamedDomainObjectContainer<T>.withMainCompilationIfExists(crossinline action: T.() -> Unit) {
    if (names.contains(SourceSet.MAIN_SOURCE_SET_NAME)) {
        named(SourceSet.MAIN_SOURCE_SET_NAME).configure { compilation ->
            compilation.action()
        }
    }
}