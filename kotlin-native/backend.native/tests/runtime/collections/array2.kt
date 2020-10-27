/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.collections.array2

import kotlin.test.*

@Test fun runTest() {
    val byteArray = Array<Byte>(5, { i -> (i * 2).toByte() })
    byteArray.map { println(it) }

    val intArray = Array<Int>(5, { i -> i * 4 })
    println(intArray.sum())
}