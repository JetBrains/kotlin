/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.idea.serialize.IdeaExtrasSerializer
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasTypeOf

@Suppress("UNCHECKED_CAST")
object TestIdeaExtrasSerializationExtension : IdeaExtrasSerializationExtension {

    val ignoredStringKey = extrasKeyOf<String>("ignored")
    val anySerializableKey = extrasKeyOf<Any>("serializable")

    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaExtrasSerializer<T>? = when {
        key == ignoredStringKey -> null
        key == anySerializableKey -> IdeaExtrasSerializer.javaIoSerializable<Any>()
        key.type == extrasTypeOf<String>() -> TestIdeaStringExtrasSerializer
        key.type == extrasTypeOf<Int>() -> TestIdeaIntExtrasSerializer
        else -> null
    } as? IdeaExtrasSerializer<T>
}
