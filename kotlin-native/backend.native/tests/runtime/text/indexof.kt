/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.text.indexof

import kotlin.test.*

@Test fun runTest() {
    var str = "Hello World!!"    // for indexOf String
    var ch = 'a'                 // for indexOf Char

    assertEquals(6, str.indexOf("World", 0))
    assertEquals(6, str.indexOf("World", -1))

    assertEquals(-1, str.indexOf(ch, 0))

    str = "Kotlin/Native"
    assertEquals(-1, str.indexOf("/", str.length + 1))
    assertEquals(-1, str.indexOf("/", Int.MAX_VALUE))
    assertEquals(str.length, str.indexOf("", Int.MAX_VALUE))
    assertEquals(1, str.indexOf("", 1))

    assertEquals(8, str.indexOf(ch, 1))
    assertEquals(-1, str.indexOf(ch, str.length - 1))

    str = ""
    assertEquals(-1, str.indexOf("a", -3))
    assertEquals(0, str.indexOf("", 0))

    assertEquals(-1, str.indexOf(ch, -3))
    assertEquals(-1, str.indexOf(ch, 10))

    ch = 0.toChar()
    assertEquals(-1, str.indexOf(ch, -3))
    assertEquals(-1, str.indexOf(ch, 10))
}