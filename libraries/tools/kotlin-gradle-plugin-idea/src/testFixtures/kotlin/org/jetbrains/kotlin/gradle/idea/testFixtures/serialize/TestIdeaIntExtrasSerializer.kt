/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext
import java.nio.ByteBuffer

object TestIdeaIntExtrasSerializer : IdeaExtrasSerializer<Int> {
    override fun serialize(context: IdeaSerializationContext, value: Int): ByteArray {
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array()
    }

    override fun deserialize(context: IdeaSerializationContext, data: ByteArray): Int {
        return ByteBuffer.wrap(data).int
    }
}
