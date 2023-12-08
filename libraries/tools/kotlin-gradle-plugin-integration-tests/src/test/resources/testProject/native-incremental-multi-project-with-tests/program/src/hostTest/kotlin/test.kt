/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

@Test
fun testFoo() {
    assertEquals(42, foo())
}

@Test
fun testBar() {
    kotlin.test.assertEquals(84, bar())
}

@Test
fun testBaz() {
    kotlin.test.assertEquals(83, baz())
}

@Test
fun testQux() {
    kotlin.test.assertEquals(125, qux())
}
