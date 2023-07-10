/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.proto.tcs

import org.jetbrains.kotlin.gradle.idea.proto.AbstractSerializationTest
import org.jetbrains.kotlin.gradle.idea.proto.generated.tcs.IdeaKotlinBinaryCapabilityProto
import org.jetbrains.kotlin.gradle.idea.tcs.IdeaKotlinBinaryCapability
import org.junit.Test

class IdeaKotlinBinaryCapabilitySerializationTest : AbstractSerializationTest<IdeaKotlinBinaryCapability>() {
    override fun serialize(value: IdeaKotlinBinaryCapability): ByteArray = IdeaKotlinBinaryCapabilityProto(value).toByteArray()

    override fun deserialize(data: ByteArray): IdeaKotlinBinaryCapability =
        IdeaKotlinBinaryCapability(IdeaKotlinBinaryCapabilityProto.parseFrom(data))

    @Test
    fun `sample 0`() = testSerialization(
        IdeaKotlinBinaryCapability(
            group = "myGroup",
            name = "myModule",
            version = "myVersion",
        )
    )

    @Test
    fun `sample 1`() = testSerialization(
        IdeaKotlinBinaryCapability(
            group = "myGroup",
            name = "myModule",
            version = null,
        )
    )
}
