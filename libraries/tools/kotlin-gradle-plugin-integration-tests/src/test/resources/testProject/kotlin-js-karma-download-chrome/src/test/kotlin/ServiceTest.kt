/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example

import kotlin.test.Test
import kotlin.test.assertEquals

class ServiceTest {
    @Test
    fun serviceTest() {
        assertEquals("value", value())
    }

    @Test
    fun serviceFailTest() {
        assertEquals("value12", value())
    }
}