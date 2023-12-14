/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

enum class Foo {
    A;
    enum class Bar { C }
}

fun box(): String {
    assertEquals("A", Foo.A.toString())
    assertEquals("C", Foo.Bar.C.toString())

    return "OK"
}