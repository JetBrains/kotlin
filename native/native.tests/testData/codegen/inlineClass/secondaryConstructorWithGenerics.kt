/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

// Based on KT-42649.
inline class IC<T>(val value: List<T>) {
    constructor(value: T) : this(listOf(value))
}

fun box(): String {
    assertEquals("abc", IC("abc").value.singleOrNull())

    return "OK"
}
