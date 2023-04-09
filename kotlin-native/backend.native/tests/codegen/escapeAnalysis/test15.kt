/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test15

class A {
    var s = "zzz"
}

fun foo(a: A, s: String) {
    a.s = s
}

fun bar(f: Boolean): String {
    val a = A()
    val b = if (f) a else A()
    foo(b, "qxx")
    return b.s
}

fun main() = println(bar(true))