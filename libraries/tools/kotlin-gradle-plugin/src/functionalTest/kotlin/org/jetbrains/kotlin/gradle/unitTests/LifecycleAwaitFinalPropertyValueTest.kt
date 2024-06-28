/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.IllegalLifecycleException
import org.jetbrains.kotlin.gradle.plugin.KotlinPluginLifecycle.Stage.*
import org.jetbrains.kotlin.gradle.util.buildProjectWithMPP
import org.jetbrains.kotlin.gradle.util.runLifecycleAwareTest
import org.jetbrains.kotlin.gradle.utils.newProperty
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class LifecycleAwaitFinalPropertyValueTest {
    private val project = buildProjectWithMPP()

    @Test
    fun `test - awaitFinalValue`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()

        launchInStage(FinaliseDsl.previousOrThrow.previousOrThrow) {
            property.set(1)
        }

        launchInStage(FinaliseDsl.previousOrThrow.previousOrThrow) {
            assertEquals(1, property.get())
            property.set(2)
        }

        assertEquals(EvaluateBuildscript, currentKotlinPluginLifecycle().stage)
        assertEquals(2, property.awaitFinalValue())
        assertEquals(AfterFinaliseDsl, currentKotlinPluginLifecycle().stage)
    }

    @Test
    fun `test - changing value after finalized`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()
        property.set(1)

        launch {
            property.awaitFinalValue()
        }

        launchInStage(FinaliseDsl.nextOrThrow) {
            assertFailsWith<IllegalStateException> { property.set(2) }
        }
    }

    @Test
    fun `test - getting value is an idempotent operation`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()
        property.set(1)

        launch {
            assertEquals(1, property.awaitFinalValue())
            assertEquals(1, property.awaitFinalValue())
        }
    }

    @Test
    fun `test - awaitFinalValueOrThrow - throws when no value set`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()
        launch {
            assertFailsWith<IllegalLifecycleException> { property.awaitFinalValueOrThrow() }
        }
    }

    @Test
    fun `test - awaitFinalValueOrThrow - returns when convention value is set`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()
        property.convention(1)
        launch {
            assertEquals(1, property.awaitFinalValueOrThrow())
        }
    }

    @Test
    fun `test - awaitFinalValueOrThrow - returns when value is set`() = project.runLifecycleAwareTest {
        val property = project.newProperty<Int>()
        property.set(1)
        launch {
            assertEquals(1, property.awaitFinalValueOrThrow())
        }
    }

    @Test
    fun `test - creating a property - after finaliseDsl stage already passed`() = project.runLifecycleAwareTest {
        launchInStage(KotlinPluginLifecycle.Stage.last) {
            val property = project.newProperty<String>()
            assertNull(property.awaitFinalValue())
            assertFails { property.set("") }
        }
    }

    @Test
    fun `test - creating a property - in finaliseIn stage`() = project.runLifecycleAwareTest {
        launchInStage(KotlinPluginLifecycle.Stage.FinaliseDsl) {
            val property = project.newProperty<String>()
            assertNull(property.awaitFinalValue())
            assertFails { property.set("") }
        }
    }
}
