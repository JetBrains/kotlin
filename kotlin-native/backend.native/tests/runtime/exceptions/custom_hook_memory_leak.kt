/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

import kotlin.native.concurrent.*

data class C(val x: Int)

fun main() {
    Platform.isMemoryLeakCheckerActive = true

    val c = C(42)
    setUnhandledExceptionHook({ _: Throwable ->
        println("Hook ${c.x}")
    }.freeze())

    throw Error("an error")
}
