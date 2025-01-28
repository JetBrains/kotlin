/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin

/**
 * Returns an empty array of the specified type [T].
 */
public actual inline fun <reified T> emptyArray(): Array<T> =
        @Suppress("UNCHECKED_CAST")
        (arrayOfNulls<T>(0) as Array<T>)
