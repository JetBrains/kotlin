/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun sum(a:Float, b:Int) = a + b

fun box(): String {
    assertEquals(3.0F, sum(1.0F, 2))
    return "OK"
}
