/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(): String {
    val res1 = foo(Any())
    if (res1 != null) return "FAIL 1: $res1"
    val res2 = foo("zzz")
    if (res2 != null) return "FAIL 2: $res2"
    val res3 = foo("42")
    if (res3 != 42)  return "FAIL 3: $res3"

    return "OK"
}

fun foo(value: Any): Int? {
    if (value is CharSequence) {
        try {
            return value.toString().toInt()
        } catch (e: NumberFormatException) {
            return null
        }
    }
    else {
        return null
    }
}