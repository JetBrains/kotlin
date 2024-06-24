/*
 * Copyright 2016-2024 JetBrains s.r.o.
 * Use of this source code is governed by the Apache 2.0 License that can be found in the LICENSE.txt file.
 */

package kotlinx.validation

import kotlinx.validation.api.klib.KlibTarget
import kotlinx.validation.api.klib.konanTargetNameMapping
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import java.io.Serializable

/**
 * Converts [KotlinTarget] to a [KlibTarget].
 */
public fun KotlinTarget.toKlibTarget(): KlibTarget = KlibTarget(extractUnderlyingTarget(this), targetName)

private fun extractUnderlyingTarget(target: KotlinTarget): String {
    if (target is KotlinNativeTarget) {
        return konanTargetNameMapping[target.konanTarget.name]!!
    }
    return when (target.platformType) {
        KotlinPlatformType.js -> "js"
        KotlinPlatformType.wasm -> when ((target as KotlinJsIrTarget).wasmTargetType) {
            KotlinWasmTargetType.WASI -> "wasmWasi"
            KotlinWasmTargetType.JS -> "wasmJs"
            else -> throw IllegalStateException("Unreachable")
        }
        else -> throw IllegalArgumentException("Unsupported platform type: ${target.platformType}")
    }
}

/**
 * Information about a generated klib dump.
 */
public class KlibDumpMetadata(
    /**
     * The target the dump was generated for.
     */
    @get:Input
    public val target: KlibTarget,

    /**
     * Path to a resulting dump file.
     *
     * If a dump file was not generated for a particular [target] (which may
     * happen for an empty project, a project having only test targets,
     * or a project that has no sources for a particular target),
     * [KlibDumpMetadata] will not force an error.
     * Instead, a dependent task will be skipped.
     */
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:PathSensitive(PathSensitivity.RELATIVE)
    public val dumpFile: RegularFileProperty
) : Serializable
