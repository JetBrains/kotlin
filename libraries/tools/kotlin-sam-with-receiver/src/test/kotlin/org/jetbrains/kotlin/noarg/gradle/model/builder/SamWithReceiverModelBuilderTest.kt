/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.noarg.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.SamWithReceiver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SamWithReceiverModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = SamWithReceiverModelBuilder()
        assertTrue(modelBuilder.canBuild(SamWithReceiver::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}