// OUTPUT_DATA_FILE: inBaseClass.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class A(val a: Int)

open class B {
    lateinit var a: A
}

class C: B() {
    fun foo() { a = A(42) }
}

fun box(): String {
    val c = C()
    c.foo()
    println(c.a.a)

    return "OK"
}
