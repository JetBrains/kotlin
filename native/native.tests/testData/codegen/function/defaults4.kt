// OUTPUT_DATA_FILE: defaults4.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class A {
    open fun foo(x: Int = 42) = println(x)
}

open class B : A()

class C : B() {
    override fun foo(x: Int) = println(x + 1)
}

fun box(): String {
    C().foo()

    return "OK"
}
