/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.KotlinWasmTargetType
import org.jetbrains.kotlin.gradle.targets.js.binaryen.BinaryenExec

interface KotlinWasmTargetDsl : KotlinTarget {
    val wasmTargetType: KotlinWasmTargetType?

    fun applyBinaryen() = applyBinaryen { }
    fun applyBinaryen(body: BinaryenExec.() -> Unit)
    fun applyBinaryen(fn: Action<BinaryenExec>) {
        applyBinaryen {
            fn.execute(this)
        }
    }
}