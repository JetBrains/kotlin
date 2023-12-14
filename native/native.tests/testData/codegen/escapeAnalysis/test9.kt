/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
// TODO: check mentioned debug output of escape analyser
import kotlin.test.*

class H(val x: Int)

// Note: intentional infinite recursion for F(String). Don't try to execute the code.
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

fun box(): String {
    // When uncommented, execution of the following line would fall into infinite recursion
    // assertEquals(0, foo(A(), F("zzz")).x)
    return "OK"
}