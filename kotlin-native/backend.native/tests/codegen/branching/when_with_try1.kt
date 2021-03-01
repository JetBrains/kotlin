/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.branching.when_with_try1

import kotlin.test.*

@Test fun runTest() {
    println(foo(Any()))
    println(foo("zzz"))
    println(foo("42"))
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