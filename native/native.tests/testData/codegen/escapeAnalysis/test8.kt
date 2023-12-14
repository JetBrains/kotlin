/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TODO: check mentioned debug output of escape analyser

// Note: intentional infinite recursion for F(String). Don't try to execute the code.
class F(val s: String) {
    var g = F("OK")
}

class A {
    var f = F("qzz")
}

// ----- Agressive -----
// PointsTo:
//     P0.f -> D0
//     RET.v@lue -> P0.f
//     RET.v@lue -> D0
//     RET.v@lue -> D0.g
//     D0.g -> P0.f
//     D0.g -> D0
// Escapes:
// ----- Passive -----
// PointsTo:
//     P0.f -> D0
//     RET.v@lue -> P0.f
//     RET.v@lue -> D0
//     RET.v@lue -> D0.g
//     D0.g -> P0.f
//     D0.g -> D0
//     D0.g -> D2
//     D1 -> D0
//     D2 -> D0
// Escapes: D1 D2
fun foo(a: A): F {
    a.f = F("zzz")
    a.f.g = a.f
    return a.f.g.g
}

fun box(): String {
    // When uncommented, execution of the following line would fall into infinite recursion
    // foo(A()).s
    return "OK"
}