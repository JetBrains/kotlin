/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

@Suppress("DEPRECATION")
@kotlin.internal.InlineOnly
internal actual inline fun collectionToArray(collection: Collection<*>): Array<Any?> =
    kotlin.jvm.internal.collectionToArray(collection)

@kotlin.internal.InlineOnly
@Suppress("UNCHECKED_CAST", "DEPRECATION")
internal actual inline fun <T> collectionToArray(collection: Collection<*>, array: Array<T>): Array<T> =
    kotlin.jvm.internal.collectionToArray(collection, array as Array<Any?>) as Array<T>

