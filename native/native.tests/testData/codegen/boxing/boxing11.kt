// OUTPUT_DATA_FILE: boxing11.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun printInt(x: Int) = println(x)

class Foo(val value: Int?) {
    fun foo() {
        printInt(if (value != null) value else 42)
    }
}

fun box(): String {
    Foo(17).foo()
    Foo(null).foo()

    return "OK"
}
