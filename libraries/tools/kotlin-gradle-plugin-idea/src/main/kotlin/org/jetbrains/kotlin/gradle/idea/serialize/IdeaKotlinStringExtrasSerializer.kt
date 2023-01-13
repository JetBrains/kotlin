/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.serialize

object IdeaKotlinStringExtrasSerializer : IdeaKotlinExtrasSerializer<String> {
    override fun serialize(context: IdeaKotlinSerializationContext, value: String): ByteArray {
        return value.encodeToByteArray()
    }

    override fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray): String {
        return data.decodeToString()
    }
}
