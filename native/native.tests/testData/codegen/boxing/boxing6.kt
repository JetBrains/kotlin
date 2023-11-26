// OUTPUT_DATA_FILE: boxing6.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)

fun foo(arg: Any) {
    printInt(arg as? Int ?: 16)
}

fun box(): String {
    foo(42)
    foo("Hello")
    val nonConstInt = 42
    val nonConstString = "Hello"
    foo(nonConstInt)
    foo(nonConstString)

    return "OK"
}
