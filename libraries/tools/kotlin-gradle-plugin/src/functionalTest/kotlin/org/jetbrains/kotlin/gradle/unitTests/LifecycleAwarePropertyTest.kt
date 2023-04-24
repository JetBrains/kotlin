/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.currentKotlinPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launchInStage
import org.jetbrains.kotlin.gradle.plugin.newKotlinPluginLifecycleAwareProperty
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.test.*

class LifecycleAwarePropertyTest {
    private val project = buildProjectWithMPP()

    @Test
    fun `test - awaitFinalValue`() = project.runLifecycleAwareTest {
        val property by project.newKotlinPluginLifecycleAwareProperty<Int>(AfterFinaliseRefinesEdges)
        assertTrue(property.isKotlinPluginLifecycleAware())

        launchInStage(FinaliseRefinesEdges.previousOrThrow) {
            property.set(1)
        }

        launchInStage(FinaliseRefinesEdges) {
            assertEquals(1, property.get())
            property.set(2)
        }

        assertEquals(EvaluateBuildscript, currentKotlinPluginLifecycle().stage)
        assertEquals(2, property.awaitFinalValue())
        assertEquals(AfterFinaliseRefinesEdges, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - awaitFinalValue - on non lifecycle aware property`() = project.runLifecycleAwareTest {
        val property = project.newProperty<String>()
        assertFalse(property.isKotlinPluginLifecycleAware())
        assertEquals(KotlinPluginLifecycle.Stage.EvaluateBuildscript, currentKotlinPluginLifecycle().stage)
        assertNull(property.awaitFinalValue())
        assertEquals(KotlinPluginLifecycle.Stage.FinaliseDsl, currentKotlinPluginLifecycle().stage)
        assertFails { property.set("x") }
    }

    @Test
    fun `test - changing value after finalized`() = project.runLifecycleAwareTest {
        val property by project.newKotlinPluginLifecycleAwareProperty<Int>(AfterEvaluateBuildscript)
        property.set(1)

        launchInStage(AfterEvaluateBuildscript) {
            assertFailsWith<IllegalStateException> { property.set(2) }
        }
    }

    @Test
    fun `test - creating a property - after finaliseIn stage already passed`() = project.runLifecycleAwareTest {
        launchInStage(KotlinPluginLifecycle.Stage.last) {
            val property by project.newKotlinPluginLifecycleAwareProperty<String>(Companion.first)

            /* Property was finalised immediately */
            assertFailsWith<IllegalStateException> { property.set("Expected to fail") }
            assertNull(property.orNull)
        }
    }

    @Test
    fun `test - creating a property - in finaliseIn stage`() = project.runLifecycleAwareTest {
        launchInStage(KotlinPluginLifecycle.Stage.FinaliseDsl) {
            val property by project.newKotlinPluginLifecycleAwareProperty<String>(FinaliseDsl)

            /* Property was finalised immediately */
            assertFailsWith<IllegalStateException> { property.set("Expected to fail") }
            assertNull(property.orNull)
        }
    }
}
