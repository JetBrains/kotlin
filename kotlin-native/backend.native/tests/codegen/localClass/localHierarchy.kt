/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.localClass.localHierarchy

import kotlin.test.*

fun foo(s: String): String {
    open class Local {
        fun f() = s
    }

    open class Derived: Local() {
        fun g() = f()
    }

    return Derived().g()
}

@Test fun runTest() {
    println(foo("OK"))
}