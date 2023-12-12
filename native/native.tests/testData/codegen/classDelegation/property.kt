/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface A {
    val x: Int
}

class C: A {
    override val x: Int = 42
}

class Q(a: A): A by a

fun box(): String {
    val q = Q(C())
    val a: A = q
    if (q.x != 42) return "FAIL q.x=${q.x}"
    if (a.x != 42) return "FAIL a.x=${a.x}"
    return "OK"
}
