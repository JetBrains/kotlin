/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.native.concurrent.*
import kotlin.native.internal.*

fun setHookAndThrow() {
    val hook = { throwable: Throwable ->
        print("Kotlin hook: ${throwable::class.simpleName}. Runnable state: ${Debugging.isThreadStateRunnable}")
    }
    if (Platform.memoryModel != MemoryModel.EXPERIMENTAL) {
        hook.freeze()
    }

    setUnhandledExceptionHook(hook)

    throw Exception("Error")
}
