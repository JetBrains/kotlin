/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:OptIn(FreezingIsDeprecated::class)

package runtime.workers.freeze_disabled

import kotlin.test.*
import kotlin.native.concurrent.*

// this test is ran with disabled freezing, so no mutability checks are done

class A(var x:Int)

@Test
fun testClassNotFrozen(){
    val a = A(1)
    a.freeze()
    a.x = 2
    assertEquals(a.x, 2)
}

@Test
fun testArrayNotFrozen(){
    val a = arrayOf(1, 2, 3, 4, 5)
    a.freeze()
    a[0] = 6
    assertEquals(a[0], 6)
}
