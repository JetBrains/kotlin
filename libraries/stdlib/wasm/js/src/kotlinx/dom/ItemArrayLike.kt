/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.w3c.dom

public external interface ItemArrayLike<out T : JsAny?> : JsAny {
    public val length: Int
    public fun item(index: Int): T?
}

public fun <T : JsAny?> ItemArrayLike<T>.asList(): List<T> = object : AbstractList<T>() {
    override val size: Int get() = this@asList.length

    @Suppress("UNCHECKED_CAST")
    override fun get(index: Int): T = when (index) {
        in 0..lastIndex -> this@asList.item(index) as T
        else -> throw IndexOutOfBoundsException("index $index is not in range [0..$lastIndex]")
    }
}