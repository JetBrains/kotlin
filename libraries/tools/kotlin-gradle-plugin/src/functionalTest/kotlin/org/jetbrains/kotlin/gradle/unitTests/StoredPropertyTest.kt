/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.utils.storedExtrasProperty
import org.jetbrains.kotlin.gradle.utils.storedProjectProperty
import org.jetbrains.kotlin.tooling.core.HasMutableExtras
import org.jetbrains.kotlin.tooling.core.MutableExtras
import org.jetbrains.kotlin.tooling.core.mutableExtrasOf
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class StoredPropertyTest {

    private data class Value(val id: Any)

    private val Project.projectNameProperty by storedProjectProperty { Value(project.name) }

    private val atomic = AtomicInteger(0)
    private val Project.counterProperty by storedProjectProperty { Value(atomic.getAndIncrement()) }

    private class Scope(val id: Any) {
        val Project.myProperty by storedProjectProperty { Value(id) }
    }

    private class TestEntity(val id: Any) : HasMutableExtras {
        override val extras: MutableExtras = mutableExtrasOf()
    }

    private val TestEntity.myProperty by storedExtrasProperty { Value(id) }

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
}
