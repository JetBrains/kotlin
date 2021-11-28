/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.inline.localFunctionInInitializerBlock

import kotlin.test.*

class Foo {
    init {
        bar()
    }
}

inline fun bar() {
    println({ "Ok" }())
}

@Test fun runTest() {
    Foo()
}