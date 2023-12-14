/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class Foo(val a: String) {

    fun test() = a
}

inline fun test(a: String, b: () -> String, c: () -> String, d: () -> String, e: String): String {
    return a + b() + c() + d() + e
}

var effects = ""

fun create(a: String): Foo {
    effects += a
    return Foo(a)
}

fun create2(a: String, f: () -> String): Foo {
    effects += a
    return Foo(a)
}

fun box(): String {
    val result = test(create("A").a, create("B")::a, create("C")::test, create2("E", create("D")::test)::test, create("F").a)
    if (effects != "ABCDEF") return "fail 1: $effects"

    return if (result == "ABCEF") "OK" else "fail 2: $result"
}
