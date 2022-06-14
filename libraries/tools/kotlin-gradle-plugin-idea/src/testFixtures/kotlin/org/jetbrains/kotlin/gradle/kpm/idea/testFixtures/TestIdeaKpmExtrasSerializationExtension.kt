/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.kpm.idea.testFixtures

import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializationExtension
import org.jetbrains.kotlin.gradle.kpm.idea.serialize.IdeaKpmExtrasSerializer
import org.jetbrains.kotlin.tooling.core.Extras
import org.jetbrains.kotlin.tooling.core.extrasKeyOf
import org.jetbrains.kotlin.tooling.core.extrasTypeOf

@Suppress("UNCHECKED_CAST")
object TestIdeaKpmExtrasSerializationExtension : IdeaKpmExtrasSerializationExtension {

    val ignoredStringKey = extrasKeyOf<String>("ignored")
    val anySerializableKey = extrasKeyOf<Any>("serializable")

    override fun <T : Any> serializer(key: Extras.Key<T>): IdeaKpmExtrasSerializer<T>? = when {
        key == ignoredStringKey -> null
        key == anySerializableKey -> IdeaKpmExtrasSerializer.javaIoSerializable<Any>()
        key.type == extrasTypeOf<String>() -> TestIdeaKpmStringExtrasSerializer
        key.type == extrasTypeOf<Int>() -> TestIdeaKpmIntExtrasSerializer
        else -> null
    } as? IdeaKpmExtrasSerializer<T>
}
