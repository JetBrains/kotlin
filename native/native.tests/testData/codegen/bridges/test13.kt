// OUTPUT_DATA_FILE: test13.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


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

fun box(): String {
    val b = B()
    val a = A<Int>()
    b.bar(42)
    a.bar(42)

    return "OK"
}
