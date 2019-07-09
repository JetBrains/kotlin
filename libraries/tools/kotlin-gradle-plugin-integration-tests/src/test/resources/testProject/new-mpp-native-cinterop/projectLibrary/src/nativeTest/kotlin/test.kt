/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package example.cinterop.project

import kotlin.test.*
import example.cinterop.project.stdio.*

@Test
fun projectTest() {
    projectPrint("Project test")
}

@Test
fun compilerOptsTest() {
    assertEquals(2, getNumber())
}
