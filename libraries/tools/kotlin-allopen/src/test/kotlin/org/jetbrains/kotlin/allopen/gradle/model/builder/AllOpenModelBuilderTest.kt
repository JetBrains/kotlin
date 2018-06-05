/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.AllOpen
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AllOpenModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = AllOpenModelBuilder()
        assertTrue(modelBuilder.canBuild(AllOpen::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}