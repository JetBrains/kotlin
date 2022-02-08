/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.escapeAnalysis.test12

class A(val s: String)

// ----- Agressive -----
// PointsTo:
//     RET.v@lue -> P0.inte$tines
// Escapes:
// ----- Passive -----
// PointsTo:
//     RET.v@lue -> P0.inte$tines
// Escapes:
fun foo(arr: Array<A>) = arr[0]

fun main() = println(foo(arrayOf(A("zzz"))).s)