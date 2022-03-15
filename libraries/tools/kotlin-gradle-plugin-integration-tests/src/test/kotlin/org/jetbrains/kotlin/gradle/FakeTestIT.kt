/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.junit.Test
import kotlin.test.assertTrue

/**
 * Exists so job on CI will not fail with "no tests found" error.
 */
class FakeTestIT : BaseGradleIT() {
    @Test
    fun name() {
        assertTrue(1 == 1)
    }
}