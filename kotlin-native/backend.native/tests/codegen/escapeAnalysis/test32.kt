/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test32

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

fun bar(b: B) {
    val a = A("qxx")
    foo(a, b, true)
}

fun baz(): B {
    val b = B()
    try {
        bar(b)
    } catch(t: Throwable) { }
    return b
}

fun main() = println(baz())