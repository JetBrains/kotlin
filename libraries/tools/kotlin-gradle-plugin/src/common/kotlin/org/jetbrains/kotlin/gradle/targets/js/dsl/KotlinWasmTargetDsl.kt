/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.dsl

import groovy.lang.Closure
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.targets.js.d8.D8Exec

interface KotlinWasmSubTargetContainerDsl : KotlinTarget {
    val d8: KotlinWasmD8Dsl

    val isD8Configured: Boolean

    fun whenD8Configured(body: KotlinWasmD8Dsl.() -> Unit)
}

interface KotlinWasmTargetDsl : KotlinJsTargetDsl {
    fun d8() = d8 { }
    fun d8(body: KotlinWasmD8Dsl.() -> Unit)
    fun d8(fn: Closure<*>) {
        d8 {
            ConfigureUtil.configure(fn, this)
        }
    }
}

interface KotlinWasmD8Dsl : KotlinJsSubTargetDsl {
    fun runTask(body: D8Exec.() -> Unit)
}