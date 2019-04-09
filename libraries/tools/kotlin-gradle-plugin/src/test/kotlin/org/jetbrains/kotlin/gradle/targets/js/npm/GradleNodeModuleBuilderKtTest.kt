/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.js.npm

import org.junit.Test

import org.junit.Assert.*

class GradleNodeModuleBuilderKtTest {
    @Test
    fun fixSemver() {
        assertEquals("1.3.0-SNAPSHOT", fixSemver("1.3-SNAPSHOT"))
        assertEquals("1.2.3-RC1-1234", fixSemver("1.2.3-RC1-1234"))
    }
}