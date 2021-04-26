/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.hash0

import kotlin.test.*

@Test fun runTest() {
    println(239.hashCode())
    println((-1L).hashCode())
    println('a'.hashCode())
    println(1.0f.hashCode())
    println(1.0.hashCode())
    println(true.hashCode())
    println(false.hashCode())
    println(Any().hashCode() != Any().hashCode())
    val a = CharArray(5)
    a[0] = 'H'
    a[1] = 'e'
    a[2] = 'l'
    a[3] = 'l'
    a[4] = 'o'
    println("Hello".hashCode() == a.concatToString(0, 5).hashCode())
}