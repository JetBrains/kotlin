/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.model

import org.jetbrains.kotlin.gradle.BaseGradleIT
import org.jetbrains.kotlin.gradle.GradleVersionRequired
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NoArgModelIT : BaseGradleIT() {
    @Test
    fun testNoArgKt18668() {
        val project = Project("noArgKt18668")
        val noArgModel = project.getModels(NoArg::class.java).getModel(":")!!
        assertEquals(1L, noArgModel.modelVersion)
        assertEquals("noArgKt18668", noArgModel.name)
        assertEquals(1, noArgModel.annotations.size)
        assertTrue(noArgModel.annotations.contains("test.NoArg"))
        assertTrue(noArgModel.presets.isEmpty())
        assertFalse(noArgModel.isInvokeInitializers)
    }

    @Test
    fun testNonNoArgProjects() {
        val project = Project("kotlinProject")
        val model = project.getModels(NoArg::class.java).getModel(":")

        assertNull(model)
    }
}