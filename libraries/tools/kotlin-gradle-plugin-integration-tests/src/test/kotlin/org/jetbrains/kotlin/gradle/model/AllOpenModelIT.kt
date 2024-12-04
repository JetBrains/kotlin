/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("'all-open' plugin model validity")
@OtherGradlePluginTests
class AllOpenModelIT : KGPBaseTest() {

    @DisplayName("Valid model is available when plugin is applied")
    @GradleTest
    fun testAllOpenSimple(gradleVersion: GradleVersion) {
        project("allOpenSimple", gradleVersion) {
            getModels<AllOpen> {
                with(getModel(":")!!) {
                    assertEquals(1L, modelVersion)
                    assertEquals("allOpenSimple", name)
                    assertEquals(1, annotations.size)
                    assertTrue(annotations.contains("lib.AllOpen"))
                    assertTrue(presets.isEmpty())
                }
            }
        }
    }

    @DisplayName("Model is not available when plugin is not applied")
    @GradleTest
    fun testNonAllOpenProjects(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            getModels<AllOpen> {
                assertNull(getModel(":"))
            }
        }
    }
}