/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package runtime.basic.hello4

import kotlin.test.*

@Test fun runTest() {
    val x = 2
    println(if (x == 2) "Hello" else "Привет")
    println(if (x == 3) "Bye" else "Пока")
 }