/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test3

class A(val s: String)
class B {
    var s: String? = null
}

// ----- Agressive -----
// PointsTo:
//     P1.s -> P0.s
//     RET.v@lue -> P0.s
// Escapes:
// ----- Passive -----
// PointsTo:
//     P1.s -> P0.s
//     RET.v@lue -> P0.s
// Escapes:
fun foo(a: A, b: B): String {
    b.s = a.s
    return a.s
}

fun main() = println(foo(A("zzz"), B()))