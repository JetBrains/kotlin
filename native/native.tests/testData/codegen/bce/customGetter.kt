/*
 * Copyright 2010-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
import kotlin.test.*

var needSmallArray = true

val array: Array<Int> = arrayOf(1)
    get() = if (needSmallArray) field else arrayOf(1, 2, 3)

fun box(): String {
    val a = array
    needSmallArray = false
    assertFailsWith<IndexOutOfBoundsException> {
        for (index in 0 until array.size) {
            a[index] = 6
        }
    }

    return "OK"
}
