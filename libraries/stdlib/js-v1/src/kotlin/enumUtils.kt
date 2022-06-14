/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

/**
 * Returns an external enum entry with specified name.
 */
public inline fun <reified T: ExternalEnum> enumValueOf(name: String): T {
    return js("Kotlin").defaultEnumValueOf(T::class, name)
}

/**
 * Returns an array containing external enum entries.
 */
public inline fun <reified T: ExternalEnum> enumValues(): Array<T> {
    return js("Kotlin").defaultEnumValues(T::class)
}