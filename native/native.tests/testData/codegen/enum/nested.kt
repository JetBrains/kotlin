/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.enum.nested

import kotlin.test.*

enum class Foo {
    A;
    enum class Bar { C }
}

@Test fun runTest() {
    println(Foo.A)
    println(Foo.Bar.C)
}