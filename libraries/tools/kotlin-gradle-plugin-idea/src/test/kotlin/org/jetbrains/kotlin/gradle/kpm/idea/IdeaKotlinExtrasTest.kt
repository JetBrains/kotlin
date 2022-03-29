/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("RemoveExplicitTypeArguments")

package org.jetbrains.kotlin.gradle.kpm.idea

import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.deserialize
import org.jetbrains.kotlin.gradle.kpm.idea.testFixtures.serialize
import org.jetbrains.kotlin.tooling.core.*
import java.io.Serializable
import kotlin.test.*

class IdeaKotlinExtrasTest {

    private data class RetainedModel(val value: Int) : Serializable
    private data class UnretainedModel(val value: Int)

    private val retainedModelKey = extrasKeyOf<RetainedModel>() + IdeaKotlinExtrasSerializer.serializable()
    private val retainedModelKeyFoo = extrasKeyOf<RetainedModel>("foo") + IdeaKotlinExtrasSerializer.serializable()
    private val retainedModelKeyBar = extrasKeyOf<RetainedModel>("bar") + IdeaKotlinExtrasSerializer.serializable()

    private val unretainedModelKey = extrasKeyOf<UnretainedModel>()
    private val unretainedModelKeyFoo = extrasKeyOf<UnretainedModel>("foo")
    private val unretainedModelKeyBar = extrasKeyOf<UnretainedModel>("bar")

    @Test
    fun `test - extras are captured when creating IdeaKotlinExtras`() {
        val extras = mutableExtrasOf(extrasKeyOf<Int>() withValue 1)
        val capturedExtras = extras.toExtras()
        val ideaKotlinExtras = IdeaKotlinExtras(extras)
        extras[extrasKeyOf<Int>("a")] = 2

        assertNotEquals(extras, capturedExtras, "Expected extras to contain additional 'a' key")
        assertNotEquals<Extras>(extras, ideaKotlinExtras)
        assertNotEquals<Extras>(ideaKotlinExtras, extras)
        assertEquals<Extras>(capturedExtras, ideaKotlinExtras)
        assertEquals<Extras>(ideaKotlinExtras, capturedExtras)
    }

    @Test
    fun `test - serializing extras`() {
        val extras = mutableExtrasOf()
        extras[retainedModelKey] = RetainedModel(1)
        extras[retainedModelKeyFoo] = RetainedModel(2)
        extras[retainedModelKeyBar] = RetainedModel(3)
        extras[unretainedModelKey] = UnretainedModel(4)
        extras[unretainedModelKeyFoo] = UnretainedModel(5)
        extras[unretainedModelKeyBar] = UnretainedModel(6)

        val deserializedContainer = IdeaKotlinExtras(extras).serialize().deserialize<IdeaKotlinExtras>()

        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])
        assertEquals(RetainedModel(2), deserializedContainer[retainedModelKeyFoo])
        assertEquals(RetainedModel(3), deserializedContainer[retainedModelKeyBar])
        assertNull(deserializedContainer[unretainedModelKey])
        assertNull(deserializedContainer[unretainedModelKeyFoo])
        assertNull(deserializedContainer[unretainedModelKeyBar])
    }

    @Test
    fun `test - serializing extras twice`() {
        val extras = mutableExtrasOf()
        extras[retainedModelKey] = RetainedModel(1)
        extras[unretainedModelKey] = UnretainedModel(4)

        val deserializedContainer = IdeaKotlinExtras(extras).serialize().deserialize<IdeaKotlinExtras>()
        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])
        assertNull(deserializedContainer[unretainedModelKey])

        val twiceDeserializedContainer = deserializedContainer.serialize().deserialize<IdeaKotlinExtras>()
        assertEquals(RetainedModel(1), twiceDeserializedContainer[retainedModelKey])
        assertNull(twiceDeserializedContainer[unretainedModelKey])
    }

    @Test
    fun `test - accessing deserialized extras without serializer`() {
        val extras = mutableExtrasOf()
        extras[retainedModelKey] = RetainedModel(1)
        extras[retainedModelKeyFoo] = RetainedModel(2)

        val deserializedContainer = IdeaKotlinExtras(extras).serialize().deserialize<IdeaKotlinExtras>()
        assertEquals(RetainedModel(1), deserializedContainer[retainedModelKey])

        /* Accessing something already deserialized without deserializer: Returning null, because the keys do not match */
        assertNull(deserializedContainer[Extras.Key(retainedModelKey.id)])

        /* Accessing something not deserialized without deserializer: Cannot deserialize: Null to be lenient */
        assertNull(deserializedContainer[Extras.Key(retainedModelKeyFoo.id)])

        /* Accessing something previously requested but now with deserializer */
        assertEquals(RetainedModel(2), deserializedContainer[retainedModelKeyFoo])

        /* Now accessing retainedModelFoo without serializer should behave like above */
        assertNull(deserializedContainer[Extras.Key(retainedModelKeyFoo.id)])
    }
}
