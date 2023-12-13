/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test2

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0.s
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0.s
// Escapes:
fun foo(a: A) = a.s

fun main() = println(foo(A("zzz")))