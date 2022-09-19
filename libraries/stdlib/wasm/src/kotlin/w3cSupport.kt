/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js

external class Promise<T>

@PublishedApi
internal val undefined: Nothing? = null

/**
 * Exposes the JavaScript [eval function](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/eval) to Kotlin.
 */
@JsFun("(s) => eval(s)")
external fun js(code: String): Dynamic

external interface ItemArrayLike<out T> {
    val length: Int
    fun item(index: Int): T?
}

fun <T> ItemArrayLike<T>.asList(): List<T> = object : AbstractList<T>() {
    override val size: Int get() = this@asList.length

    override fun get(index: Int): T = when (index) {
        in 0..lastIndex -> this@asList.item(index) as T
        else -> throw IndexOutOfBoundsException("index $index is not in range [0..$lastIndex]")
    }
}