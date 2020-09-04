/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

@PublishedApi
internal fun <T : Enum<T>> enumValuesIntrinsic(): Array<T> =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
internal fun <T : Enum<T>> enumValueOfIntrinsic(@Suppress("UNUSED_PARAMETER") name: String): T =
    throw IllegalStateException("Should be replaced by compiler")

@PublishedApi
internal fun safePropertyGet(self: dynamic, getterName: String, propName: String): dynamic {
    val getter = self[getterName]
    return if (getter != null) getter.call(self) else self[propName]
}

@PublishedApi
internal fun safePropertySet(self: dynamic, setterName: String, propName: String, value: dynamic) {
    val setter = self[setterName]
    if (setter != null) setter.call(self, value) else self[propName] = value
}