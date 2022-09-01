/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.container.assignment.gradle.model.builder

import org.jetbrains.kotlin.gradle.model.ValueContainerAssignment
import org.junit.Assert
import org.junit.Test

class ValueContainerAssignmentModelBuilderTest {

    @Test
    fun testCanBuild() {
        val modelBuilder = ValueContainerAssignmentModelBuilder()
        Assert.assertTrue(modelBuilder.canBuild(ValueContainerAssignment::class.java.name))
        Assert.assertFalse(modelBuilder.canBuild("wrongModel"))
    }
}