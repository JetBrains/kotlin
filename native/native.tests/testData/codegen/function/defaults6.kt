/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class Foo(val x: Int = 42)
class Bar : Foo()

fun box(): String {
    assertEquals(42, Bar().x)
    return "OK"
}