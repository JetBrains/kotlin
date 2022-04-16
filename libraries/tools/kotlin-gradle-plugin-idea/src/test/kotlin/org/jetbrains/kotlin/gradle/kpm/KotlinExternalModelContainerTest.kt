/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm

import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.deserialize
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.serialize
import java.io.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame
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

    @Test
    fun `test - accessing deserialized container without serializer`() {
        val container = KotlinExternalModelContainer.mutable()
        container[retainedModelKey] = RetainedModel(1)
        container[retainedModelKeyFoo] = RetainedModel(2)

        val deserializedContainer = container.serialize().deserialize<KotlinExternalModelContainer>()
        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])

        /* Accessing something already deserialized without deserializer: Returning null, because the keys do not match */
        assertNull(deserializedContainer[KotlinExternalModelKey(retainedModelKey.id)])

        /* Accessing something not deserialized without deserializer: Cannot deserialize: Null to be lenient */
        assertNull(deserializedContainer[KotlinExternalModelKey(retainedModelKeyFoo.id)])

        /* Accessing something previously requested but now with deserializer */
        assertEquals(RetainedModel(2), deserializedContainer[retainedModelKeyFoo])

        /* Now accessing retainedModelFoo without serializer should behave like above */
        assertNull(deserializedContainer[KotlinExternalModelKey(retainedModelKeyFoo.id)])
    }

    @Test
    fun `test - empty containers are equal`() {
        val container1 = KotlinExternalModelContainer.mutable()
        val container2 = KotlinExternalModelContainer.mutable()

        assertNotSame(container1, container2)
        assertEquals(container1, container2)
        assertEquals(container2, container1)
        assertEquals<KotlinExternalModelContainer>(KotlinExternalModelContainer.Empty, container1)
        assertEquals<KotlinExternalModelContainer>(container1, KotlinExternalModelContainer.Empty)
        assertEquals<KotlinExternalModelContainer>(KotlinExternalModelContainer.Empty, container2)
        assertEquals<KotlinExternalModelContainer>(container2, KotlinExternalModelContainer.Empty)

        assertEquals(container1.hashCode(), container2.hashCode())
        assertEquals(container1.hashCode(), KotlinExternalModelContainer.Empty.hashCode())
    }
}
