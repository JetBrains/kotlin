/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializer
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaSerializationContext

object TestIdeaStringExtrasSerializer : IdeaExtrasSerializer<String> {
    override fun serialize(context: IdeaSerializationContext, value: String): ByteArray = value.encodeToByteArray()
    override fun deserialize(context: IdeaSerializationContext, data: ByteArray) = data.decodeToString()
}
