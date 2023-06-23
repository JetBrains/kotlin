/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.configurationResult
import org.jetbrains.kotlin.gradle.plugin.sources.internal
import org.jetbrains.kotlin.gradle.util.assertContainsDependencies
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.tooling.core.extrasReadWriteProperty
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class KotlinMultiplatformSourceSetConventionsTest {
    private var KotlinSourceSet.testMarker by extrasReadWriteProperty<String>("testMarker")

    @Test
    fun `test - invoke`() = buildProjectWithMPP().runLifecycleAwareTest {

        val mainConfigurationCalled = AtomicBoolean(false)
        val testConfigurationCalled = AtomicBoolean(false)

        multiplatformExtension.apply {
            jvm()

            sourceSets.jvmMain {
                testMarker = "jvmMain"
                assertFalse(mainConfigurationCalled.getAndSet(true))
            }

            sourceSets.jvmTest {
                testMarker = "jvmTest"
                assertFalse(testConfigurationCalled.getAndSet(true))
            }

            configurationResult.await()
            assertEquals("jvmMain", sourceSets.jvmMain.get().testMarker)
            assertEquals("jvmTest", sourceSets.jvmTest.get().testMarker)
        }
    }

    @Test
    fun `test - languageSettings`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm()

            sourceSets.jvmMain.languageSettings {
                this.optIn("jvmMain.optIn")
            }

            sourceSets.jvmTest.languageSettings {
                this.optIn("jvmTest.optIn")
            }

            assertEquals(setOf("jvmMain.optIn"), sourceSets.jvmMain.get().languageSettings.optInAnnotationsInUse)
            assertEquals(setOf("jvmTest.optIn"), sourceSets.jvmTest.get().languageSettings.optInAnnotationsInUse)
        }
    }

    @Test
    fun `test - dependencies`() = buildProjectWithMPP().runLifecycleAwareTest {
        multiplatformExtension.apply {
            jvm()

            sourceSets.jvmMain.dependencies {
                implementation("my:jvmMain:library")
            }

            sourceSets.jvmTest.dependencies {
                implementation("my:jvmTest:library")
            }

            assertContainsDependencies(
                sourceSets.jvmMain.get().implementationConfigurationName,
                "my:jvmMain:library"
            )

            assertContainsDependencies(
                sourceSets.jvmTest.get().implementationConfigurationName,
                "my:jvmTest:library"
            )
        }
    }

}