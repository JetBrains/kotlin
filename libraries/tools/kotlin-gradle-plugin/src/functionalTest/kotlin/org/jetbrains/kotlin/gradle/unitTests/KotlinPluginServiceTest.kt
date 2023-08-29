/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.gradle.api.Project
import org.gradle.kotlin.dsl.provideDelegate
import org.jetbrains.kotlin.gradle.plugin.kotlinPluginService
import org.jetbrains.kotlin.gradle.util.buildProject
import kotlin.test.*

class KotlinPluginServiceTest {

    data class MyService(val id: Int)
    data class MyOtherService(val id: Int)

    private val Project.kotlinService0 by kotlinPluginService {
        MyService(0).also(constructed::add)
    }

    private val Project.kotlinService1 by kotlinPluginService {
        MyService(1).also(constructed::add)
    }

    private val Project.kotlinOtherService0 by kotlinPluginService {
        MyOtherService(0).also(constructed::add)
    }

    private val Project.kotlinOtherService1 by kotlinPluginService {
        MyOtherService(1).also(constructed::add)
    }

    object ScopeA {
        val Project.kotlinService by kotlinPluginService { "scopeA" }
    }

    object ScopeB {
        val Project.kotlinService by kotlinPluginService { "scopeB" }
    }

    private val Project.foo by kotlinPluginService { "foo" }

    private val project = buildProject()

    private val constructed = mutableListOf<Any>()

    @Test
    fun `smoke test`() {
        assertEquals(MyService(0), project.kotlinService0)
        assertSame(project.kotlinService0, project.kotlinService0)
        assertEquals(listOf<Any>(MyService(0)), constructed)

        assertEquals(MyService(1), project.kotlinService1)
        assertEquals(MyService(1), project.kotlinService1)
        assertSame(project.kotlinService1, project.kotlinService1)
        assertEquals(listOf<Any>(MyService(0), MyService(1)), constructed)

        assertEquals(MyOtherService(0), project.kotlinOtherService0)
        assertSame(project.kotlinOtherService0, project.kotlinOtherService0)
        assertEquals(listOf(MyService(0), MyService(1), MyOtherService(0)), constructed)

        assertEquals(MyOtherService(1), project.kotlinOtherService1)
        assertSame(project.kotlinOtherService1, project.kotlinOtherService1)
        assertEquals(listOf(MyService(0), MyService(1), MyOtherService(0), MyOtherService(1)), constructed)
    }

    @Test
    fun `test - fails when property does not start with kotlin`() {
        assertFailsWith<AssertionError> { project.foo }
    }

    @Test
    fun `test - property with same name and type in different scope`() {
        assertEquals("scopeA", with(ScopeA) { project.kotlinService })
        assertEquals("scopeB", with(ScopeB) { project.kotlinService })
    }
}
