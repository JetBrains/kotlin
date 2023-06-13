/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test33

class A(val s: String)

class B {
    var a: A? = null
}

fun foo(a: A, b: B, f: Boolean) {
    if (f) {
        b.a = a
        throw IllegalStateException("zzz")
    }
}

fun bar(a: A, b: B) {
    val a2 = A("qxx")
    foo(a2, b, true)
    b.a = a
}

fun baz(): B {
    val a = A("zzz")
    val b = B()
    try {
        bar(a, b)
    } catch(t: Throwable) { }
    return b
}

fun main() = println(baz())