/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_errors

import kotlin.native.internal.*
import kotlin.test.*

@Test fun runTest() {
    val frame = runtimeGetCurrentFrame()
    // Negative step.
    try {
        for (i in 0 .. 4 step -2) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 0 until 4 step -2) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 4 downTo 0 step -2) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    // Zero step.
    try {
        for (i in 0 .. 4 step 0) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 0 until 4 step 0) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 4 downTo 0 step 0) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    // Two steps, one is negative.
    try {
        for (i in 0 .. 4 step -2 step 3) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 0 until 4 step -2 step 3) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    try {
        for (i in 4 downTo 0 step -2 step 3) print(i); println()
        throw AssertionError()
    } catch (e: IllegalArgumentException) {
        assertTrue(runtimeCurrentFrameIsEqual(frame))
    }

    println("OK")
}
