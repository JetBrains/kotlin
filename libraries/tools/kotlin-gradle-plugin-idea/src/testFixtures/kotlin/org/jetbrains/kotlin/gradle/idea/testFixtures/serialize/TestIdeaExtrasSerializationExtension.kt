/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.idea.testFixtures.serialize

import org.jetbrains.kotlin.gradle.idea.serialize.*
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasTypeOf

@Suppress("UNCHECKED_CAST")
object TestIdeaExtrasSerializationExtension : IdeaKotlinExtrasSerializationExtension {

    val ignoredStringKey = extrasKeyOf<String>("ignored")
    val anySerializableKey = extrasKeyOf<Any>("serializable")

    override fun <T> serializer(key: Extras.Key<T>): IdeaKotlinExtrasSerializer<T>? = when {
        key == ignoredStringKey -> null
        key == anySerializableKey -> IdeaKotlinExtrasSerializer.javaIoSerializable<Any>()
        key.type == extrasTypeOf<String>() -> IdeaKotlinStringExtrasSerializer
        key.type == extrasTypeOf<Int>() -> IdeaKotlinIntExtrasSerializer
        key.type == extrasTypeOf<Boolean>() -> IdeaKotlinBooleanExtrasSerializer
        else -> null
    } as? IdeaKotlinExtrasSerializer<T>
}
