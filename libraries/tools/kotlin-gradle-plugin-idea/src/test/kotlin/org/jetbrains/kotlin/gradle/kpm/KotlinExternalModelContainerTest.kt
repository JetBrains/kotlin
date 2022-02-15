/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import deserialize
import serialize
import java.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class KotlinExternalModelContainerTest {

    private data class RetainedModel(val value: Int) : Serializable
    private data class UnretainedModel(val value: Int)

    private val retainedModelKey = KotlinExternalModelKey<RetainedModel>(KotlinExternalModelSerializer.serializable())
    private val retainedModelKeyFoo = KotlinExternalModelKey<RetainedModel>("foo", KotlinExternalModelSerializer.serializable())
    private val retainedModelKeyBar = KotlinExternalModelKey<RetainedModel>("bar", KotlinExternalModelSerializer.serializable())

    private val unretainedModelKey = KotlinExternalModelKey<UnretainedModel>()
    private val unretainedModelKeyFoo = KotlinExternalModelKey<UnretainedModel>("foo")
    private val unretainedModelKeyBar = KotlinExternalModelKey<UnretainedModel>("bar")

    @Test
    fun `test - attach simple values`() {
        val container = KotlinExternalModelContainer.mutable()
        container[retainedModelKey] = RetainedModel(1)
        container[retainedModelKeyFoo] = RetainedModel(2)
        container[retainedModelKeyBar] = RetainedModel(3)
        container[unretainedModelKey] = UnretainedModel(4)
        container[unretainedModelKeyFoo] = UnretainedModel(5)
        container[unretainedModelKeyBar] = UnretainedModel(6)

        assertEquals(RetainedModel(1), container[retainedModelKey])
        assertEquals(RetainedModel(2), container[retainedModelKeyFoo])
        assertEquals(RetainedModel(3), container[retainedModelKeyBar])
        assertEquals(UnretainedModel(4), container[unretainedModelKey])
        assertEquals(UnretainedModel(5), container[unretainedModelKeyFoo])
        assertEquals(UnretainedModel(6), container[unretainedModelKeyBar])
    }

    @Test
    fun `test - accessing missing value`() {
        val container = KotlinExternalModelContainer.mutable()
        assertNull(container[retainedModelKey])
        assertNull(container[unretainedModelKey])
    }

    @Test
    fun `test - serializing container`() {
        val container = KotlinExternalModelContainer.mutable()
        container[retainedModelKey] = RetainedModel(1)
        container[retainedModelKeyFoo] = RetainedModel(2)
        container[retainedModelKeyBar] = RetainedModel(3)
        container[unretainedModelKey] = UnretainedModel(4)
        container[unretainedModelKeyFoo] = UnretainedModel(5)
        container[unretainedModelKeyBar] = UnretainedModel(6)

        val deserializedContainer = container.serialize().deserialize<KotlinExternalModelContainer>()

        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])
        assertEquals(RetainedModel(2), deserializedContainer[retainedModelKeyFoo])
        assertEquals(RetainedModel(3), deserializedContainer[retainedModelKeyBar])
        assertNull(deserializedContainer[unretainedModelKey])
        assertNull(deserializedContainer[unretainedModelKeyFoo])
        assertNull(deserializedContainer[unretainedModelKeyBar])
    }

    @Test
    fun `test - serializing container twice`() {
        val container = KotlinExternalModelContainer.mutable()
        container[retainedModelKey] = RetainedModel(1)
        container[unretainedModelKey] = UnretainedModel(4)

        val deserializedContainer = container.serialize().deserialize<KotlinExternalModelContainer>()
        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])
        assertNull(deserializedContainer[unretainedModelKey])

        val twiceDeserializedContainer = deserializedContainer.serialize().deserialize<KotlinExternalModelContainer>()
        assertEquals(RetainedModel(1), twiceDeserializedContainer[retainedModelKey])
        assertNull(twiceDeserializedContainer[unretainedModelKey])
    }
}
