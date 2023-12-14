/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo():Int = 1
fun bar():Int = 2
fun sum():Int = foo() + bar()

fun box(): String {
    assertEquals(3, sum())
    return "OK"
}
