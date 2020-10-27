/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.memory.escape0

import kotlin.test.*

//fun foo1(arg: String) : String = foo0(arg)
fun foo1(arg: Any) : Any = foo0(arg)

fun foo0(arg: Any) : Any = Any()

var global : Any =  Any()

fun foo0_escape(arg: Any) : Any{
    global = arg
    return Any()
}

class Node(var previous: Node?)

fun zoo3() : Node {
    var current = Node(null)
    for (i in 1 .. 5) {
        current = Node(current)
    }
    return current
}

fun zoo4(arg: Int) : Any {
    var a = Any()
    var b = Any()
    var c = Any()
    a = b
    val x = 3
    a = when {
        x < arg -> b
        else -> c
    }
    return a
}

fun zoo5(arg: Any) : Any{
    foo1(arg)
    return arg
}

fun zoo6(arg: Any) : Any {
    return zoo7(arg, "foo", 11)
}

fun zoo7(arg1: Any, arg2: Any, selector: Int) : Any {
    return if (selector < 2) arg1 else arg2;
}

@Test fun runTest() {
    //val z = zoo7(Any(), Any(), 1)
    val x = zoo5(Any())
    //println(bar(foo1(foo2("")), foo2(foo1(""))))
}
