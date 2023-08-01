/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec

interface KotlinWasmSubTargetContainerDsl : KotlinTarget {
    val d8: KotlinWasmD8Dsl

    val isD8Configured: Boolean

    fun whenD8Configured(body: KotlinWasmD8Dsl.() -> Unit)

    fun whenBinaryenApplied(body: (BinaryenExec.() -> Unit) -> Unit)
}

interface KotlinWasmJsTargetDsl : KotlinWasmTargetDsl, KotlinJsTargetDsl {
    fun d8() = d8 { }
    fun d8(body: KotlinWasmD8Dsl.() -> Unit)
    fun d8(fn: Action<KotlinWasmD8Dsl>) {
        d8 {
            fn.execute(this)
        }
    }

    fun applyBinaryen() = applyBinaryen { }
    fun applyBinaryen(body: BinaryenExec.() -> Unit)
    fun applyBinaryen(fn: Action<BinaryenExec>) {
        applyBinaryen {
            fn.execute(this)
        }
    }
}

interface KotlinWasmD8Dsl : KotlinJsSubTargetDsl {
    fun runTask(body: D8Exec.() -> Unit)
}