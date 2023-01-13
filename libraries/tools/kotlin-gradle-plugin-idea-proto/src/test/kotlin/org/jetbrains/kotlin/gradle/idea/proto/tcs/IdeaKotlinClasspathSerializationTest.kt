/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinClasspath
import org.jetbrains.kotlin.gradle.idea.testFixtures.tcs.TestIdeaKotlinInstances
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class IdeaKotlinClasspathSerializationTest : AbstractSerializationTest<IdeaKotlinClasspath>() {
    override fun serialize(value: IdeaKotlinClasspath): ByteArray {
        return value.toByteArray()
    }

    override fun deserialize(data: ByteArray): IdeaKotlinClasspath {
        return IdeaKotlinClasspath(data) ?: fail("Failed to deserialize IdeaKotlinClasspath")
    }

    @Test
    fun `test - simpleClasspath`() = testSerialization(TestIdeaKotlinInstances.simpleClasspath)

    @Test
    fun `test  - emptyClasspath`() = testSerialization(TestIdeaKotlinInstances.emptyClasspath)

    @Test
    fun `test - empty ByteArray`() {
        assertEquals(IdeaKotlinClasspath(), deserialize(byteArrayOf()))
        if (serialize(IdeaKotlinClasspath()).isNotEmpty())
            fail("Expected empty classpath to serialize into empty ByteArray")
    }
}