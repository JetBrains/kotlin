/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.AllOpen
import org.jetbrains.kotlin.gradle.testbase.KGPBaseTest
import org.jetbrains.kotlin.gradle.testbase.OtherGradlePluginTests
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@DisplayName("Allopen plugin model builder")
@OtherGradlePluginTests
class AllOpenModelBuilderTest : KGPBaseTest() {

    @DisplayName("can build")
    @Test
    fun testCanBuild() {
        val modelBuilder = AllOpenModelBuilder()
        assertTrue(modelBuilder.canBuild(AllOpen::class.java.name))
        assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}
