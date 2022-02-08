/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.test13

import kotlin.test.*

open class A<T> {
    open fun T.foo() {
        println(this.toString())
    }

    fun bar(x: T) {
        x.foo()
    }
}

open class B: A<Int>() {
    override fun Int.foo() {
        println(this)
    }
}

@Test fun runTest() {
    val b = B()
    val a = A<Int>()
    b.bar(42)
    a.bar(42)
}