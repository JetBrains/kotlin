/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
import project.Main
import kotlin.test.*

class MainTest {
    @Test
    fun mySimpleTest() {
        assertEquals("instant", Main().value())
    }
}