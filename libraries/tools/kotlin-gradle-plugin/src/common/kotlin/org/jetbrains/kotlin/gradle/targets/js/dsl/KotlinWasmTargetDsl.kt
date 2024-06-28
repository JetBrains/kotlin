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
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsBinaryContainer
import org.jetbrains.kotlin.gradle.targets.js.ir.KotlinJsIrCompilation

interface KotlinWasmTargetDsl : KotlinTarget, HasBinaries<KotlinJsBinaryContainer> {
    val wasmTargetType: KotlinWasmTargetType?

    @Suppress("DEPRECATION")
    @Deprecated("Binaryen is enabled by default. This call is redundant.")
    fun applyBinaryen() = applyBinaryen { }

    @Deprecated("Binaryen is enabled by default. This call is redundant.")
    fun applyBinaryen(body: BinaryenExec.() -> Unit)

    @Suppress("DEPRECATION")
    @Deprecated("Binaryen is enabled by default. This call is redundant.")
    fun applyBinaryen(fn: Action<BinaryenExec>) {
        applyBinaryen {
            fn.execute(this)
        }
    }

    override val compilations: NamedDomainObjectContainer<KotlinJsIrCompilation>

    override val binaries: KotlinJsBinaryContainer
}