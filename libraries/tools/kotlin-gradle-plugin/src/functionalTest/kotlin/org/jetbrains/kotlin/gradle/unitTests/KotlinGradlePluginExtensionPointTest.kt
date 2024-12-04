/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("FunctionName")

package org.jetbrains.kotlin.gradle.unitTests

import org.jetbrains.kotlin.gradle.util.buildProject
import org.jetbrains.kotlin.gradle.util.set
import org.jetbrains.kotlin.gradle.plugin.KotlinGradlePluginExtensionPoint
import kotlin.test.Test
import kotlin.test.assertEquals

class KotlinGradlePluginExtensionPointTest {

    @Test
    fun `test - simple extension point`() {
        val extensionPoint = KotlinGradlePluginExtensionPoint<String>()
        val projectA = buildProject()
        val projectB = buildProject()

        extensionPoint.register(projectA, "a")
        extensionPoint.register(projectB, "b")

        assertEquals(listOf("a"), extensionPoint[projectA])
        assertEquals(listOf("b"), extensionPoint[projectB])

        extensionPoint.register(projectA, "a2")
        assertEquals(listOf("a", "a2"), extensionPoint[projectA])
    }

    @Test
    fun `test - overwrite`() {
        val extensionPoint = KotlinGradlePluginExtensionPoint<Int>()
        val project = buildProject()

        extensionPoint.register(project, 0)
        extensionPoint.register(project, 1)
        assertEquals(listOf(0, 1), extensionPoint[project])

        extensionPoint.set(project, listOf(2, 3))
        assertEquals(listOf(2, 3), extensionPoint[project])
    }
}
