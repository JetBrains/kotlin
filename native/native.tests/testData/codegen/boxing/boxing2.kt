// OUTPUT_DATA_FILE: boxing2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)
fun printBoolean(x: Boolean) = println(x)
fun printUInt(x: UInt) = println(x)

fun foo(arg: Any) {
    if (arg is Int)
        printInt(arg)
    else if (arg is Boolean)
        printBoolean(arg)
    else if (arg is UInt)
        printUInt(arg)
    else
        println("other")
}

fun box(): String {
    foo(1)
    foo(2u)
    foo(true)
    foo("Hello")
    val nonConstInt = 1
    val nonConstUInt = 2u
    val nonConstBool = true
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstUInt)
    foo(nonConstBool)
    foo(nonConstString)

    return "OK"
}
