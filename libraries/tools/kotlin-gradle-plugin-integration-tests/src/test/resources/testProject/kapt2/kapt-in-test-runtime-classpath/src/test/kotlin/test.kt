/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.example

import org.junit.Test
import java.util.ServiceLoader

class Tests {
    @Test
    fun testServiceLoading() {
        ServiceLoader.load(SomeInterface::class.java).singleOrNull() ?: error("Cannot load SomeInterface implementation, perhaps the service declaration couldn't be found")
    }
}