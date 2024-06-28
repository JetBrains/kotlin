/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinBinaryAttributesProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryAttributes
import org.junit.Test

class IdeaKotlinBinaryAttributesSerializationTest : AbstractSerializationTest<IdeaKotlinBinaryAttributes>() {
    override fun serialize(value: IdeaKotlinBinaryAttributes): ByteArray = IdeaKotlinBinaryAttributesProto(value).toByteArray()

    override fun deserialize(data: ByteArray): IdeaKotlinBinaryAttributes =
        IdeaKotlinBinaryAttributes(IdeaKotlinBinaryAttributesProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinBinaryAttributes(emptyMap())
    )

    @Test
    fun `sample 1`() = testSerialization(
        IdeaKotlinBinaryAttributes(mapOf("a" to "valueA", "b" to "valueB"))
    )
}
