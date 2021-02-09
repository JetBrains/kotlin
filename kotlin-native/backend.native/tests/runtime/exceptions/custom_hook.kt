/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

import kotlin.native.concurrent.*

fun setHookLegacyMM(hook: ReportUnhandledExceptionHook) : ReportUnhandledExceptionHook? {
    assertFailsWith<InvalidMutabilityException> {
        setUnhandledExceptionHook { _ -> println("wrong") }
    }

    return setUnhandledExceptionHook(hook.freeze())
}

fun setHookNewMM(hook: ReportUnhandledExceptionHook) : ReportUnhandledExceptionHook? {
    return setUnhandledExceptionHook(hook)
}

fun setHook(hook: ReportUnhandledExceptionHook) : ReportUnhandledExceptionHook? {
    return when (kotlin.native.Platform.memoryModel) {
        kotlin.native.MemoryModel.EXPERIMENTAL -> setHookNewMM(hook)
        else -> setHookLegacyMM(hook)
    }
}

fun main() {
    val x = 42
    val old = setHook {
        throwable: Throwable -> println("value $x: ${throwable::class.simpleName}")
    }

    assertNull(old)
    throw Error("an error")
}
