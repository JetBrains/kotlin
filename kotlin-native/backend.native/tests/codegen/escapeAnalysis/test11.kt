/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test11

class F(val x: Int)

class A(val s: String) {
    var f = F(0)
}

var f: F? = null

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0.f
//     D0 -> P0.f
// Escapes: D0
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0.f
//     D0 -> P0.f
// Escapes: D0
fun foo(a: A): F {
    f = a.f
    return a.f
}

fun main() = println(foo(A("zzz")))