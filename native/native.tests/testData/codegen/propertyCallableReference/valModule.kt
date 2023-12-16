/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val x = 42

fun box(): String {
    val p = ::x
    assertEquals(42, p.get())

    return "OK"
}