/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.test.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinIntExtrasSerializer.deserialize
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinIntExtrasSerializer.serialize
import org.jetbrains.kotlin.gradle.idea.testFixtures.serialize.TestIdeaKotlinSerializationContext
import kotlin.test.Test
import kotlin.test.assertEquals

class IdeaKotlinIntExtrasSerializerTest {

    @Test
    fun `test - values`() {
        val context = TestIdeaKotlinSerializationContext()
        assertEquals(2411, deserialize(context, serialize(context, 2411)))
        assertEquals(-2411, deserialize(context, serialize(context, -2411)))
        assertEquals(Int.MAX_VALUE, deserialize(context, serialize(context, Int.MAX_VALUE)))
        assertEquals(Int.MIN_VALUE, deserialize(context, serialize(context, Int.MIN_VALUE)))
    }
}