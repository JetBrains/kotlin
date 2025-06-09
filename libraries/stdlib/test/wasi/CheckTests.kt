/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package test.wasi

import test.TestPlatform
import test.current
import kotlin.test.Test

class CheckTests {
    @Test
    fun checkIfExecuted() {
        if (TestPlatform.current == TestPlatform.WasmWasi) throw RuntimeException("Yay! We're running!")
    }
}
