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

class SamWithReceiverModelIT : BaseGradleIT() {
    @Test
    fun testSamWithReceiverSimple() {
        val project = Project("samWithReceiverSimple")
        val samWithReceiverModel = project.getModels(SamWithReceiver::class.java).getModel(":")!!
        assertEquals(1L, samWithReceiverModel.modelVersion)
        assertEquals("samWithReceiverSimple", samWithReceiverModel.name)
        assertEquals(1, samWithReceiverModel.annotations.size)
        assertTrue(samWithReceiverModel.annotations.contains("lib.SamWithReceiver"))
        assertTrue(samWithReceiverModel.presets.isEmpty())
    }

    @Test
    fun testNonSamWithReceiverProjects() {
        val project = Project("kotlinProject")
        val model = project.getModels(SamWithReceiver::class.java).getModel(":")

        assertNull(model)
    }
}