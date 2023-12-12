/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

fun box(): String {
    val array = Array(10) { 0 }

    assertFailsWith<IndexOutOfBoundsException> {
        array.forEachIndexed { index, _ ->
            array[index + 1] = 1
        }
    }
    return "OK"
}
