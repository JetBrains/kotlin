/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.allopen.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.AllOpen
import org.junit.Assert
import org.junit.Test

class AllOpenModelBuilderTest {
    @Test
    fun testCanBuild() {
        val modelBuilder = AllOpenModelBuilder()
        Assert.assertTrue(modelBuilder.canBuild(AllOpen::class.java.name))
        Assert.assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}
