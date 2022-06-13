/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmInstances
import kotlin.test.*

class IdeaKpmProjectContainerTest {

    @Test
    fun `test - binary container - equality`() {
        val container1 = IdeaKpmProjectContainer(byteArrayOf(1))
        val container2 = IdeaKpmProjectContainer(byteArrayOf(1))
        val container3 = IdeaKpmProjectContainer(byteArrayOf(1, 2))

        assertEquals(container1, container2)
        assertNotEquals(container2, container3)
    }

    @Test
    fun `test - instance container - equality`() {
        val container1 = IdeaKpmProjectContainer(TestIdeaKpmInstances.simpleProject)
        val container2 = IdeaKpmProjectContainer(TestIdeaKpmInstances.simpleProject.copy())
        val container3 = IdeaKpmProjectContainer(TestIdeaKpmInstances.simpleProject.copy(gradlePluginVersion = "some.other.version"))

        assertEquals(container1, container2)
        assertNotEquals(container2, container3)
    }

    @Test
    fun `test - binary container - instanceOrNull`() {
        assertNull(IdeaKpmProjectContainer(byteArrayOf()).instanceOrNull)
        assertNotNull(IdeaKpmProjectBinaryContainer::class.java.getMethod("getInstanceOrNull"))
    }

    @Test
    fun `test - instance container - instanceOrNull`() {
        assertSame(TestIdeaKpmInstances.simpleProject, IdeaKpmProjectContainer(TestIdeaKpmInstances.simpleProject).instanceOrNull)
        assertNotNull(IdeaKpmProjectInstanceContainer::class.java.getMethod("getInstanceOrNull"))
    }

    @Test
    fun `test - binary container - binaryOrNull`() {
        val binary = byteArrayOf()
        assertEquals(binary, IdeaKpmProjectContainer(binary).binaryOrNull)
        assertNotNull(IdeaKpmProjectBinaryContainer::class.java.getMethod("getBinaryOrNull"))
    }

    @Test
    fun `test - instance container - binaryOrNull`() {
        assertNull(IdeaKpmProjectContainer(TestIdeaKpmInstances.simpleProject).binaryOrNull)
        assertNotNull(IdeaKpmProjectInstanceContainer::class.java.getMethod("getBinaryOrNull"))
    }
}
