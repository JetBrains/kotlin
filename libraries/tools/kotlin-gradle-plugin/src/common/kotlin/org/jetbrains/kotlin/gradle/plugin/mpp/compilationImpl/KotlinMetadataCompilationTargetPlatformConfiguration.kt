/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.mpp.compilationImpl

import org.jetbrains.kotlin.gradle.dsl.awaitMetadataTarget
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinProjectSetupCoroutine
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSharedNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.sources.awaitPlatformCompilations
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrTarget
import org.jetbrains.kotlin.gradle.targets.metadata.awaitMetadataCompilationsCreated
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

internal val KotlinMetadataCompilationTargetPlatformConfiguration = KotlinProjectSetupCoroutine {
    val metadataCompilations = project.multiplatformExtension
        .awaitMetadataTarget()
        .awaitMetadataCompilationsCreated()

    for (metadataCompilation in metadataCompilations) {
        val platformCompilations = metadataCompilation.defaultSourceSet.internal.awaitPlatformCompilations()
        // native compiler arguments don't have `-Xtarget-platform` flag
        if (metadataCompilation is KotlinSharedNativeCompilation) continue
        metadataCompilation.compileTaskProvider.configure { task ->
            val targetsArguments = platformCompilations.mapNotNull {
                when (it.platformType) {
                    KotlinPlatformType.androidJvm, KotlinPlatformType.jvm -> "JVM"
                    KotlinPlatformType.js -> "JS"
                    KotlinPlatformType.wasm -> {
                        when ((it.target as? KotlinJsIrTarget)?.wasmTargetType) {
                            KotlinWasmTargetType.WASI -> "WasmWasi"
                            KotlinWasmTargetType.JS, null -> "WasmJs"
                        }
                    }
                    KotlinPlatformType.native -> "Native"
                    else -> null
                }
            }.distinct()
            (task as? KotlinCompileCommon)?.targetPlatformArg?.set(targetsArguments)
        }
    }
}
