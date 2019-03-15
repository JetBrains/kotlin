/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AllOpenModelIT : BaseGradleIT() {
    @Test
    fun testAllOpenSimple() {
        val project = Project("allOpenSimple")
        val allOpenModel = project.getModels(AllOpen::class.java).getModel(":")!!
        assertEquals(1L, allOpenModel.modelVersion)
        assertEquals("allOpenSimple", allOpenModel.name)
        assertEquals(1, allOpenModel.annotations.size)
        assertTrue(allOpenModel.annotations.contains("lib.AllOpen"))
        assertTrue(allOpenModel.presets.isEmpty())
    }

    @Test
    fun testNonAllOpenProjects() {
        val project = Project("kotlinProject")
        val model = project.getModels(AllOpen::class.java).getModel(":")

        assertNull(model)
    }
}