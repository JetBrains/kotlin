/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.KotlinAndroidExtension
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class KotlinAndroidExtensionModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = KotlinAndroidExtensionModelBuilder()
        assertTrue(modelBuilder.canBuild(KotlinAndroidExtension::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}