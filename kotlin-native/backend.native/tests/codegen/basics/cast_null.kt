/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.basics.cast_null

import kotlin.test.*
import kotlin.native.internal.*

@Test
fun runTest() {
    testCast(null, false)
    testCastToNullable(null, true)
    testCastToNullable(TestKlass(), true)
    testCastToNullable("", false)
    testCastNotNullableToNullable(TestKlass(), true)
    testCastNotNullableToNullable("", false)

    println("Ok")
}

class TestKlass

fun ensure(b: Boolean) {
    if (!b) {
        println("Error")
    }
}

fun testCast(x: Any?, expectSuccess: Boolean) {
    val frame = runtimeGetCurrentFrame()
    try {
        x as TestKlass
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastToNullable(x: Any?, expectSuccess: Boolean) {
    val frame = runtimeGetCurrentFrame()
    try {
        x as TestKlass?
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun testCastNotNullableToNullable(x: Any, expectSuccess: Boolean) {
    val frame = runtimeGetCurrentFrame()
    try {
        x as TestKlass?
    } catch (e: Throwable) {
        assertEquals(frame, runtimeGetCurrentFrame())
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}