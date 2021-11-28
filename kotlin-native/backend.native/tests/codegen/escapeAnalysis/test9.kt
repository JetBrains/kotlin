/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test9

class H(val x: Int)

class F(val s: String) {
    var g = F("")
    var h = H(0)
}

class A {
    var f = F("qzz")
}

// ----- Agressive -----
// PointsTo:
//     P0.f -> P1.g
//     RET.v@lue -> P1.g.h
// Escapes:
// ----- Passive -----
// PointsTo:
//     P0.f -> P1.g
//     P1.g.h -> D0
//     RET.v@lue -> P1.g.h
// Escapes: D0
fun foo(a: A, f: F): H {
    a.f = f.g
    a.f.h = H(42)
    return f.g.h
}

fun main() = println(foo(A(), F("zzz")).x)