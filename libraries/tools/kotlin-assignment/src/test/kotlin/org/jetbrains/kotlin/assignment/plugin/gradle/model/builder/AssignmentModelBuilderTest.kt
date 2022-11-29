/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.assignment.plugin.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.Assignment
import org.junit.Assert
import org.junit.Test

class AssignmentModelBuilderTest {

    @Test
    fun testCanBuild() {
        val modelBuilder = AssignmentModelBuilder()
        Assert.assertTrue(modelBuilder.canBuild(Assignment::class.java.name))
        Assert.assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}
