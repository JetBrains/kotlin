/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.native.internal.*
import kotlin.test.*

inline fun foo() {
    val frame = runtimeGetCurrentFrame()
    try {
        try {
            throw Exception("XXX")
        } catch (e: Throwable) {
            assertTrue(runtimeCurrentFrameIsEqual(frame))
            println("Gotcha1: ${e.message}")
            throw Exception("YYY")
        }
    } catch (e: Throwable) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        println("Gotcha2: ${e.message}")
    }
}
