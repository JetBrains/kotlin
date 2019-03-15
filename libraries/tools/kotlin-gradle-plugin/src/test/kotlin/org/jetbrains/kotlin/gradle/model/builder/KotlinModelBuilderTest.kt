/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.KotlinProject
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = KotlinModelBuilder("version", null)
        assertTrue(modelBuilder.canBuild(KotlinProject::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}