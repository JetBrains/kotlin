/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test1

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0
// Escapes:
fun foo(a: A) = a

fun main() = println(foo(A("zzz")))