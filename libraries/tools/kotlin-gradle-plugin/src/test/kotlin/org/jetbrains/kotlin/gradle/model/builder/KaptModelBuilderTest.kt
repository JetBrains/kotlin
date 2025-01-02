/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.Kapt
import org.jetbrains.kotlin.test.MuteableTestRule
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KaptModelBuilderTest {
    @get:Rule val muteableTestRule = MuteableTestRule()

    @Test
    fun testCanBuild() {
        val modelBuilder = KaptModelBuilder()
        assertTrue(modelBuilder.canBuild(Kapt::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}