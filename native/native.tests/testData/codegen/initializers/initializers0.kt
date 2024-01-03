/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.initializers0

import kotlin.test.*

class A {
    init{
        println ("A::init")
    }

    val a = 1

    companion object :B(1) {
        init {
            println ("A::companion")
        }

        fun foo() {
            println("A::companion::foo")
        }
    }

    object AObj : B(){
        init {
            println("A::Obj")
        }
        fun foo() {
            println("A::AObj::foo")
        }
    }
}

@Test fun runTest() {
    println("main")
    A.foo()
    A.foo()
    A.AObj.foo()
    A.AObj.foo()
}

open class B(val a:Int, val b:Int) {
    constructor(a:Int):this (a, 0) {
        println("B::constructor(" + a.toString()+ ")")
    }
    constructor():this(0) {
        println("B::constructor()")
    }
}

