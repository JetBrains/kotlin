/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

fun box(): String {
    fun varargGetter(position:Int, vararg x: Int): Int {
        x[position] *= 5;
        return x[position]
    }

    repeat(3) {
        assertEquals(10, varargGetter(0, 2, 3, 4))
        assertEquals(10, varargGetter(0, 2, 3, 4))
        assertEquals(15, varargGetter(1, 2, 3, 4))
        assertEquals(15, varargGetter(1, 2, 3, 4))
        assertEquals(20, varargGetter(2, 2, 3, 4))
        assertEquals(20, varargGetter(2, 2, 3, 4))
        // the following assert might fail on Kotlin/WASM
        assertFailsWith<IndexOutOfBoundsException> { varargGetter(3, 2, 3, 4) }
    }

    return "OK"
}
