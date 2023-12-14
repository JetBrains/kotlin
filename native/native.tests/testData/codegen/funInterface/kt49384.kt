/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface A<T>

// https://youtrack.jetbrains.com/issue/KT-49384
class B<T> {
    init {
        mutableListOf<A<out T>>()
                .sortWith { _, _ -> 1 }
    }
}

fun box(): String {
    val b = B<Any>()
    assertEquals(b, b) // Just to ensure B is not deleted by DCE

    return "OK"
}
