/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaKotlinSerializationContext

object TestIdeaStringKotlinExtrasSerializer : IdeaKotlinExtrasSerializer<String> {
    override fun serialize(context: IdeaKotlinSerializationContext, value: String): ByteArray = value.encodeToByteArray()
    override fun deserialize(context: IdeaKotlinSerializationContext, data: ByteArray) = data.decodeToString()
}
