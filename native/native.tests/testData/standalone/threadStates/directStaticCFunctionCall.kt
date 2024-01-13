/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// FREE_COMPILER_ARGS: -opt-in=kotlin.native.internal.InternalForKotlinNative
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlin.native.runtime.Debugging
import kotlin.test.*
import kotlinx.cinterop.*

fun main() {
    val funPtr = staticCFunction { ->
        assertRunnableThreadState()
    }
    assertRunnableThreadState()
    funPtr()
    assertRunnableThreadState()
}

fun assertRunnableThreadState() {
    assertTrue(Debugging.isThreadStateRunnable)
}
