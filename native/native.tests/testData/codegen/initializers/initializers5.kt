/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

object A {
    val a = 42
    val b = A.a
}

fun box(): String {
    assertEquals(42, A.b)

    return "OK"
}