/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinBooleanExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

class IdeaKotlinBooleanExtrasSerializerTest {

    @Test
    fun `test - true`() {
        val context = TestIdeaKotlinSerializationContext()
        val data = IdeaKotlinBooleanExtrasSerializer.serialize(context, true)
        assertEquals(true, IdeaKotlinBooleanExtrasSerializer.deserialize(context, data))
        if (context.logger.reports.isNotEmpty()) fail("Unexpected reports: ${context.logger.reports}")
    }

    @Test
    fun `test - false`() {
        val context = TestIdeaKotlinSerializationContext()
        val data = IdeaKotlinBooleanExtrasSerializer.serialize(context, false)
        assertEquals(false, IdeaKotlinBooleanExtrasSerializer.deserialize(context, data))
        if (context.logger.reports.isNotEmpty()) fail("Unexpected reports: ${context.logger.reports}")
    }

    @Test
    fun `test - bad data`() {
        val context = TestIdeaKotlinSerializationContext()
        assertNull(IdeaKotlinBooleanExtrasSerializer.deserialize(context, byteArrayOf()))
        if (context.logger.reports.size != 1) fail("Expected one report. Found: ${context.logger.reports}")
    }
}
