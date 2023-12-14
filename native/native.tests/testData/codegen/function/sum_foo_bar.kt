/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo(a:Int):Int = a
fun bar(a:Int):Int = a

fun sumFooBar(a:Int, b:Int):Int = foo(a) + bar(b)

fun box(): String {
    assertEquals(5, sumFooBar(2, 3))
    return "OK"
}