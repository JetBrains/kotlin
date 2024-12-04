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

@DisplayName("Kapt plugin models")
@OtherGradlePluginTests
class KaptModelIT : KGPBaseTest() {

    @DisplayName("Valid kapt model is available when plugin is applied")
    @GradleTest
    fun testKaptSimple(gradleVersion: GradleVersion) {
        project("kapt2/simple", gradleVersion) {
            getModels<Kapt> {
                with(getModel(":")!!) {
                    assertBasics("simple")

                    assertEquals(2, kaptSourceSets.size)
                    val mainSourceSet = kaptSourceSets.first { it.name == "main" }
                    val testSourceSet = kaptSourceSets.first { it.name == "test" }

                    assertEquals(KaptSourceSet.KaptSourceSetType.PRODUCTION, mainSourceSet.type)
                    assertEquals(
                        projectPath.resolve("build/generated/source/kapt/main").toFile(),
                        mainSourceSet.generatedSourcesDirectory
                    )
                    assertEquals(
                        projectPath.resolve("build/generated/source/kaptKotlin/main").toFile(),
                        mainSourceSet.generatedKotlinSourcesDirectory
                    )
                    assertEquals(
                        projectPath.resolve("build/tmp/kapt3/classes/main").toFile(),
                        mainSourceSet.generatedClassesDirectory
                    )

                    assertEquals(KaptSourceSet.KaptSourceSetType.TEST, testSourceSet.type)
                    assertEquals(
                        projectPath.resolve("build/generated/source/kapt/test").toFile(),
                        testSourceSet.generatedSourcesDirectory
                    )
                    assertEquals(
                        projectPath.resolve("build/generated/source/kaptKotlin/test").toFile(),
                        testSourceSet.generatedKotlinSourcesDirectory
                    )
                    assertEquals(
                        projectPath.resolve("build/tmp/kapt3/classes/test").toFile(),
                        testSourceSet.generatedClassesDirectory
                    )
                }
            }
        }
    }

    @DisplayName("Model is not available when plugin is not applied")
    @GradleTest
    fun testNonJvmProjects(gradleVersion: GradleVersion) {
        project("kotlin-js-plugin-project", gradleVersion) {
            getModels<Kapt> {
                assertNull(getModel(":"))
                assertNull(getModel(":libraryProject"))
                assertNull(getModel(":mainProject"))
            }
        }
    }

    companion object {

        private fun Kapt.assertBasics(expectedName: String) {
            assertEquals(1L, modelVersion)
            assertEquals(expectedName, name)
        }
    }
}