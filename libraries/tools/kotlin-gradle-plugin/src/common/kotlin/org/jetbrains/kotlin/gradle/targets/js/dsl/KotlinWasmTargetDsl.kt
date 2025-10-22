/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.HasBinaries
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation
import org.jetbrains.kotlin.gradle.targets.wasm.binaryen.BinaryenExec

/**
 * Kotlin Wasm target configuration options.
 * The specific Wasm target is specified by [wasmTargetType].
 *
 * **Note:** This interface is not intended for implementation by build script or plugin authors.
 */
interface KotlinWasmTargetDsl : KotlinTarget, HasBinaries<KotlinJsBinaryContainer> {

    /**
     * Specifies the Wasm target (Wasi or JS) these options configure.
     */
    val wasmTargetType: KotlinWasmTargetType?

    override val compilations: NamedDomainObjectContainer<KotlinJsIrCompilation>

    override val binaries: KotlinJsBinaryContainer

    //region deprecated options
    @Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "Binaryen is enabled by default. This call is redundant. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    fun applyBinaryen() = applyBinaryen { }

    @Deprecated(
        "Binaryen is enabled by default. This call is redundant. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    fun applyBinaryen(body: BinaryenExec.() -> Unit)

    @Suppress("DEPRECATION_ERROR", "DeprecatedCallableAddReplaceWith")
    @Deprecated(
        "Binaryen is enabled by default. This call is redundant. Scheduled for removal in Kotlin 2.3.",
        level = DeprecationLevel.ERROR
    )
    fun applyBinaryen(fn: Action<BinaryenExec>) {
        applyBinaryen {
            fn.execute(this)
        }
    }
    //endregion
}
