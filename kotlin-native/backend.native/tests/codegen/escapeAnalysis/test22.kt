/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package codegen.escapeAnalysis.test22

class A {
    var s = "zzz"
}

fun foo(a: A, b: A, c: A) {
    b.s = a.s
    c.s = a.s
    a.s = "qxx"
}

fun bar(b: A, c: A) {
    foo(A(), b, c)
}

fun main() = bar(A(), A())