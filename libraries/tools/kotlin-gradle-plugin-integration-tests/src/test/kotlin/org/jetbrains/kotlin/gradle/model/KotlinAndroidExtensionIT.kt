/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.gradle.api.logging.configuration.WarningMode
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DisplayName("Kotlin-android plugin models")
@OtherGradlePluginTests
class KotlinAndroidExtensionIT : KGPBaseTest() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        androidVersion = TestVersions.AGP.AGP_34,
        warningMode = WarningMode.Summary
    )

    @DisplayName("Has valid model when plugin is applied")
    @GradleTest
    fun testAndroidExtensionsProject(gradleVersion: GradleVersion) {
        project("AndroidExtensionsProject", gradleVersion) {
            getModels<KotlinAndroidExtension> {
                with(getModel(":app")!!) {
                    assertEquals(1L, modelVersion)
                    assertEquals("app", name)
                    assertFalse(isExperimental)
                    assertEquals("hashMap", defaultCacheImplementation)
                }
            }
        }
    }

    @DisplayName("Has valid model in android multivariant project when plugin is applied")
    @GradleTest
    fun testAndroidExtensionsManyVariants(gradleVersion: GradleVersion) {
        project("AndroidExtensionsManyVariants", gradleVersion) {
            getModels<KotlinAndroidExtension> {
                with(getModel(":app")!!) {
                    assertEquals(1L, modelVersion)
                    assertEquals("app", name)
                    assertTrue(isExperimental)
                    assertEquals("hashMap", defaultCacheImplementation)
                }
            }
        }
    }

    @DisplayName("Doesn't have model when plugin is not applied")
    @GradleTest
    fun testNonAndroidExtensionsProjects(gradleVersion: GradleVersion) {
        project("kotlinProject", gradleVersion) {
            getModels<KotlinAndroidExtension> {
                assertNull(getModel(":"))
            }
        }
    }
}
