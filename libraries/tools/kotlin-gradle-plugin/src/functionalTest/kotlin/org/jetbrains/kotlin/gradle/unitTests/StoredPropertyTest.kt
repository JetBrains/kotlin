/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.extrasStoredProperty
import org.jetbrains.kotlin.gradle.utils.projectStoredProperty
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class StoredPropertyTest {

    private data class Value(val id: Any)

    private val Project.projectNameProperty by projectStoredProperty { Value(project.name) }

    private val atomic = AtomicInteger(0)
    private val Project.counterProperty by projectStoredProperty { Value(atomic.getAndIncrement()) }

    private var myState: Any? = null
    private val Project.myStateCapturingProperty by projectStoredProperty { myState }

    private class Scope(val id: Any) {
        val Project.myProperty by projectStoredProperty { Value(id) }
    }

    private class TestEntity(val id: Any) : HasMutableExtras {
        override val extras: MutableExtras = mutableExtrasOf()
    }

    private val TestEntity.myProperty by extrasStoredProperty { Value(id) }


    private val TestEntity.myStateCaptureProperty: Any? by extrasStoredProperty {
        myState
    }

    @Test
    fun `test - projectNameProperty`() {
        val projectA = buildProject({ withName("a") })
        val projectB = buildProject({ withName("b") })

        assertEquals(Value("a"), projectA.projectNameProperty)
        assertEquals(Value("b"), projectB.projectNameProperty)
        assertSame(projectA.projectNameProperty, projectA.projectNameProperty)
        assertSame(projectB.projectNameProperty, projectB.projectNameProperty)
    }

    @Test
    fun `test - counterProperty`() {
        val projectA = buildProject()
        val projectB = buildProject()

        assertEquals(Value(0), projectA.counterProperty)
        assertEquals(Value(0), projectA.counterProperty)
        assertEquals(Value(1), projectB.counterProperty)
        assertEquals(Value(1), projectB.counterProperty)
    }

    @Test
    fun `test - Scope`() {
        val project = buildProject()
        val scopeA = Scope("a")
        val scopeB = Scope("b")

        with(scopeA) {
            assertEquals(Value("a"), project.myProperty)
        }

        with(scopeB) {
            assertEquals(Value("b"), project.myProperty)
        }
    }

    @Test
    fun `test - TestEntity - myProperty`() {
        val entityA = TestEntity("a")
        val entityB = TestEntity("b")

        assertEquals(Value("a"), entityA.myProperty)
        assertEquals(Value("b"), entityB.myProperty)

        assertSame(entityA.myProperty, entityA.myProperty)
        assertSame(entityB.myProperty, entityB.myProperty)
    }

    @Test
    fun `test - nullable extras property`() {
        val entity = TestEntity("a")
        myState = null
        assertNull(entity.myStateCaptureProperty)

        myState = "Should not be captured anymore"
        assertNull(entity.myStateCaptureProperty)

        val anotherEntity = TestEntity("b")
        myState = "foo"
        assertEquals("foo", anotherEntity.myStateCaptureProperty)
    }

    @Test
    fun `test - nullable project property`() {
        val project = buildProject()
        assertNull(project.myStateCapturingProperty)

        myState = "foo"
        assertNull(project.myStateCapturingProperty)

        assertEquals("foo", buildProject().myStateCapturingProperty)
    }
}
