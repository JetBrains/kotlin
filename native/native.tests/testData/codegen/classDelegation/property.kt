// OUTPUT_DATA_FILE: property.out
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

fun test(): String {
    val q = Q(C())
    val a: A = q
    return q.x.toString() + a.x.toString()
}

fun box(): String {
    println(test())

    return "OK"
}
