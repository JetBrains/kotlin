/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

object IdeaKotlinBooleanExtrasSerializer : IdeaKotlinExtrasSerializer<Boolean> {
    override fun serialize(context: IdeaKotlinSerializationContext, value: Boolean): ByteArray {
        return byteArrayOf(if (value) 1 else 0)
    }

    override fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray): Boolean? {
        if (data.isEmpty()) {
            context.logger.error("Failed to decode Boolean from empty array")
            return null
        }

        return when (val value = data.first()) {
            0.toByte() -> false
            1.toByte() -> true
            else -> {
                context.logger.error("Failed to decode Boolean from value $value")
                null
            }
        }
    }
}