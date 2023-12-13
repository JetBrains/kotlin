/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test13

class A {
    var f: A? = null
}

fun foo(a: A, k: Int): A {
    return if (k == 0) a else foo(a.f!!, k - 1)
}

fun main() {
    val a = A()
    a.f = A()
    println(foo(a, 1))
}