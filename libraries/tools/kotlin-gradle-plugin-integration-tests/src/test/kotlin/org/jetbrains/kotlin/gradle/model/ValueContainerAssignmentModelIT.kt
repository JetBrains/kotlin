/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("Value-container-assignment plugin model")
@OtherGradlePluginTests
class ValueContainerAssignmentModelIT : KGPBaseTest() {

    @DisplayName("Valid model is available when plugin is applied")
    @GradleTest
    fun testValueContainerAssignmentSimple(gradleVersion: GradleVersion) {
        project("valueContainerAssignmentSimple", gradleVersion) {
            getModels<ValueContainerAssignment> {
                with(getModel(":")!!) {
                    assertEquals(1L, modelVersion)
                    assertEquals("valueContainerAssignmentSimple", name)
                    assertEquals(1, annotations.size)
                    assertTrue(annotations.contains("lib.ValueContainer"))
                }
            }
        }
    }

    @DisplayName("Model is not available when plugin is not applied")
    @GradleTest
    fun testNonValueContainerAssignmentProjects(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            getModels<ValueContainerAssignment> {
                assertNull(getModel(":"))
            }
        }
    }
}
