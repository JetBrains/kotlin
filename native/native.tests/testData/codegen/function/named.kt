/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo(a:Int, b:Int) = a - b

fun box(): String {
    assertEquals(18, foo(b = 24, a = 42))
    return "OK"
}