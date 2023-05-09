/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(FreezingIsDeprecated::class, ExperimentalStdlibApi::class, kotlin.native.runtime.NativeRuntimeApi::class)

import kotlin.native.concurrent.*
import kotlin.native.runtime.Debugging
import kotlinx.cinterop.staticCFunction
import threadStates.*

fun main() {
    val hook = { throwable: Throwable ->
        print("${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        hook.freeze()
    }

    setUnhandledExceptionHook(hook)

    runInNewThread(staticCFunction<Unit> { throw Error("Error") })
}