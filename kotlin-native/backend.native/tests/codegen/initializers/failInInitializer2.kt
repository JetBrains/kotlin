/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FILE: lib.kt
val x: String = computeX()

fun computeX(): String = error("zzz")

val y: String = computeY()

fun computeY(): String = "qzz"

// FILE: main.kt
import kotlin.native.internal.*
import kotlin.test.*

@OptIn(ExperimentalStdlibApi::class)
fun main() {
    val frame = runtimeGetCurrentFrame()
    try {
        println(x)
    } catch(t: IllegalStateException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        println("caught")
    }
    try {
        println(y)
    } catch(t: kotlin.native.FileFailedToInitializeException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
        println("caught2")
    }
}
