/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("No-arg plugin models")
@OtherGradlePluginTests
class NoArgModelIT : KGPBaseTest() {

    @DisplayName("Valid model with plugin is applied")
    @GradleTest
    fun testNoArgKt18668(gradleVersion: GradleVersion) {
        project("noArgKt18668", gradleVersion) {
            getModels<NoArg> {
                with(getModel(":")!!) {
                    assertEquals(1L, modelVersion)
                    assertEquals("noArgKt18668", name)
                    assertEquals(1, annotations.size)
                    assertTrue(annotations.contains("test.NoArg"))
                    assertTrue(presets.isEmpty())
                    assertFalse(isInvokeInitializers)
                }
            }
        }
    }

    @DisplayName("Model is not available when plugin is not applied")
    @GradleTest
    fun testNonNoArgProjects(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            getModels<NoArg> {
                assertNull(getModel(":"))
            }
        }
    }
}