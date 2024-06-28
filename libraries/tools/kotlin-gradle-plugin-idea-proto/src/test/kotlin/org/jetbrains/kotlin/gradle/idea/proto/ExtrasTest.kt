/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto

import org.jetbrains.kotlin.gradle.idea.proto.generated.IdeaExtrasProto
import org.jetbrains.kotlin.tooling.core.*
import kotlin.test.Test

class ExtrasTest : AbstractSerializationTest<Extras>() {

    override fun serialize(value: Extras): ByteArray = IdeaExtrasProto(value).toByteArray()
    override fun deserialize(data: ByteArray): Extras = Extras(IdeaExtrasProto.parseFrom(data))
    override fun normalize(value: Extras): Extras = value.filter { (_, value) -> value !is Ignored }.toExtras()

    class Ignored

    @Test
    fun `serialize - deserialize - sample 0`() {

        val extras = mutableExtrasOf(
            extrasKeyOf<String>() withValue "myValue",
            extrasKeyOf<String>("a") withValue "myValueA",
            extrasKeyOf<Int>() withValue 2411,
            extrasKeyOf<Ignored>() withValue Ignored()
        )

        testSerialization(extras)
    }
}
