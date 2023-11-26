// OUTPUT_DATA_FILE: inline24.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun foo() = println("foo")
fun bar() = println("bar")

inline fun baz(x: Unit = foo(), y: Unit) {}

fun box(): String {
    baz(y = bar())

    return "OK"
}
