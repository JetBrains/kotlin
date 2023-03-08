/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.plugin.awaitFinalValue
import org.jetbrains.kotlin.gradle.plugin.currentMultiplatformPluginLifecycle
import org.jetbrains.kotlin.gradle.plugin.launch
import org.jetbrains.kotlin.gradle.plugin.newLifecycleAwareProperty
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.junit.Assert.assertFalse
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LifecycleAwarePropertyTest {
    private val project = buildProjectWithMPP()

    @Test
    fun `test - awaitFinalValue`() = project.runLifecycleAwareTest {
        val property by project.newLifecycleAwareProperty<Int>(AfterFinaliseRefinesEdges)
        assertTrue(property.isLifecycleAware())

        launch(BeforeFinaliseRefinesEdges) {
            property.set(1)
        }

        launch(FinaliseRefinesEdges) {
            assertEquals(1, property.get())
            property.set(2)
        }

        assertEquals(Configure, currentMultiplatformPluginLifecycle().stage)
        assertEquals(2, property.awaitFinalValue())
        assertEquals(AfterFinaliseRefinesEdges, currentMultiplatformPluginLifecycle().stage)
    }

    @Test
    fun `test - awaitFinalValue - on non lifecycle aware property`() = project.runLifecycleAwareTest {
        val property = project.newProperty<String>()
        assertFalse(property.isLifecycleAware())
        assertFailsWith<IllegalArgumentException> { property.awaitFinalValue() }
    }

    @Test
    fun `test - changing value after finalized`() = project.runLifecycleAwareTest {
        val property by project.newLifecycleAwareProperty<Int>(AfterEvaluate)
        property.set(1)

        launch(AfterEvaluate) {
            assertFailsWith<IllegalStateException> { property.set(2) }
        }
    }
}
