/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    testCast<TestKlass>(TestKlass(), true)
    testCast<TestKlass>(null, false)
    testCastToNullable<TestKlass>(null, true)

    return "OK"
}

class TestKlass

fun ensure(b: Boolean) {
    if (!b) {
        println("Error")
    }
}

fun <T : Any> testCast(x: Any?, expectSuccess: Boolean) {
    try {
        x as T
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}

fun <T : Any> testCastToNullable(x: Any?, expectSuccess: Boolean) {
    try {
        x as T?
    } catch (e: Throwable) {
        ensure(!expectSuccess)
        return
    }
    ensure(expectSuccess)
}