/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinStringExtrasSerializer.deserialize
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinStringExtrasSerializer.serialize
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationContext
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeaKotlinStringExtrasSerializerTest {
    @Test
    fun `test - values`() {
        val context = TestIdeaKotlinSerializationContext()
        assertEquals("Sunny", deserialize(context, serialize(context, "Sunny")))
        assertEquals("☀️", deserialize(context, serialize(context, "☀️")))
    }
}
