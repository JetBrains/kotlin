/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package example.cinterop.project

import kotlin.test.*

@Test
fun testAnswer() {
    assertEquals(12, libraryAnswer())
    assertEquals(12, selfCalculatedAnswer())
    @OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
    assertEquals(5, getTestNumber())
}
