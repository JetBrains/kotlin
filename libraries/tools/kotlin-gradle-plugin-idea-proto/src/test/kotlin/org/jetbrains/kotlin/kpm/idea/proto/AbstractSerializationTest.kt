/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kpm.idea.proto

import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmSerializationContext
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.TestIdeaKpmSerializationLogger
import kotlin.test.assertEquals

abstract class AbstractSerializationTest<T : Any> : IdeaKpmSerializationContext {

    final override val logger = TestIdeaKpmSerializationLogger()
    final override val extrasSerializationExtension = TestIdeaKpmExtrasSerializationExtension

    abstract fun serialize(value: T): ByteArray
    abstract fun deserialize(data: ByteArray): T
    open fun normalize(value: T): T = value

    fun testSerialization(value: T) {
        testSerializeAndDeserializeEquals(value)
    }

    private fun testSerializeAndDeserializeEquals(value: T) {
        assertEquals(
            normalize(value), deserialize(serialize(value))
        )
    }
}
