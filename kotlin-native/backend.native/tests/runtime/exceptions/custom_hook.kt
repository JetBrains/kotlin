/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

import kotlin.native.concurrent.*
import kotlin.native.internal.*

fun mainLegacyMM() {
    assertFailsWith<InvalidMutabilityException> {
        setUnhandledExceptionHook { _ -> println("wrong") }
    }

    val x = 42
    val old = setUnhandledExceptionHook({ throwable: Throwable ->
        println("value $x: ${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }.freeze())

    assertNull(old)

    throw Error("an error")
}

fun mainExperimentalMM() {
    val unset = setUnhandledExceptionHook { _ -> println("ok") }
    assertNull(unset)

    val x = 42
    val old = setUnhandledExceptionHook { throwable: Throwable ->
        println("value $x: ${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }

    assertNotNull(old)

    throw Error("an error")
}

fun main() {
    if (Platform.memoryModel == MemoryModel.EXPERIMENTAL) {
        mainExperimentalMM()
    } else {
        mainLegacyMM()
    }
}
