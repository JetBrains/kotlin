/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

import java.nio.ByteBuffer

object IdeaKotlinIntExtrasSerializer : IdeaKotlinExtrasSerializer<Int> {
    override fun serialize(context: IdeaKotlinSerializationContext, value: Int): ByteArray {
        return ByteBuffer.allocate(Int.SIZE_BYTES).putInt(value).array()
    }

    override fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray): Int {
        return ByteBuffer.wrap(data).int
    }
}