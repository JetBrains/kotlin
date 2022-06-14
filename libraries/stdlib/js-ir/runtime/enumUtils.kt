/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Returns an external enum entry with specified name.
 */
public inline fun <reified T: ExternalEnum> enumValueOf(name: String): T {
    val externalEnum: dynamic = T::class.js
    return if (hasCustomValueOfImplementation(externalEnum)) {
        externalEnum.valueOf(name)
    } else {
        externalEnum[name]
    }
}


/**
 * Returns an array containing external enum entries.
 */
public inline fun <reified T: ExternalEnum> enumValues(): Array<T> {
    val externalEnum: dynamic = T::class.js
    return if (hasCustomValuesImplementation(externalEnum)) {
        externalEnum.values()
    } else {
        nativeObjectValues(externalEnum)
    }
}

@PublishedApi
internal fun hasCustomValueOfImplementation(jsClass: dynamic): Boolean =
    isFunction(jsClass["valueOf"]) && jsClass["valueOf"].length == 1

@PublishedApi
internal fun hasCustomValuesImplementation(jsClass: dynamic): Boolean =
    isFunction(jsClass["values"]) && jsClass["values"].length == 0

@Suppress("UNUSED_PARAMETER")
internal fun isFunction(value: dynamic): Boolean =
    js("typeof value === 'function'")
