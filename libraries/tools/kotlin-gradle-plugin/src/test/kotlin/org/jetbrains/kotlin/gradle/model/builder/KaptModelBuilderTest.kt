/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.Kapt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KaptModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = KaptModelBuilder()
        assertTrue(modelBuilder.canBuild(Kapt::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}