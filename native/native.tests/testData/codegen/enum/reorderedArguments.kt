/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// Regression test for https://github.com/JetBrains/kotlin-native/issues/1779
enum class Foo(val a: Int, val b: Int, val c: Int = 0) {
    A(a = 1, b = 0),
    B(b = 1, a = 0),
    C(c = 1, b = 0, a = 0),
    D(0, 0),
    E(1, 1, 1)
}

interface Base<T> {
    val value: T
}

enum class Bar(override val value: Foo) : Base<Foo> {
    A(Foo.A),
    B(Foo.B),
    C(Foo.C),
    D(Foo.D),
    E(Foo.E)
}

fun box(): String {

    assertEquals(Foo.A.a, 1)
    assertEquals(Foo.A.b, 0)
    assertEquals(Foo.A.c, 0)

    assertEquals(Foo.B.a, 0)
    assertEquals(Foo.B.b, 1)
    assertEquals(Foo.B.c, 0)

    assertEquals(Foo.C.a, 0)
    assertEquals(Foo.C.b, 0)
    assertEquals(Foo.C.c, 1)

    assertEquals(Foo.D.a, 0)
    assertEquals(Foo.D.b, 0)
    assertEquals(Foo.D.c, 0)

    assertEquals(Foo.E.a, 1)
    assertEquals(Foo.E.b, 1)
    assertEquals(Foo.E.c, 1)

    assertEquals(Bar.A.value.a, 1)
    assertEquals(Bar.A.value.b, 0)
    assertEquals(Bar.A.value.c, 0)

    assertEquals(Bar.B.value.a, 0)
    assertEquals(Bar.B.value.b, 1)
    assertEquals(Bar.B.value.c, 0)

    assertEquals(Bar.C.value.a, 0)
    assertEquals(Bar.C.value.b, 0)
    assertEquals(Bar.C.value.c, 1)

    assertEquals(Bar.D.value.a, 0)
    assertEquals(Bar.D.value.b, 0)
    assertEquals(Bar.D.value.c, 0)

    assertEquals(Bar.E.value.a, 1)
    assertEquals(Bar.E.value.b, 1)
    assertEquals(Bar.E.value.c, 1)

    return "OK"
}