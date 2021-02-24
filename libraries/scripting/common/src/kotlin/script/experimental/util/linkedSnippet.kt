/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.util

interface LinkedSnippet<T> {
    val previous: LinkedSnippet<T>?
    fun get(): T
}

fun <T> LinkedSnippet<T>?.toList(): List<T> = toList { it }

fun <T, R> LinkedSnippet<T>?.toList(mapper: (T) -> R): List<R> {
    val res = ArrayList<R>()
    var el = this

    while (el != null) {
        res.add(mapper(el.get()))
        el = el.previous
    }

    res.reverse()
    return res
}

fun <T> LinkedSnippet<T>?.get(): T? = this?.get()

class LinkedSnippetImpl<T>(private val _val: T, override val previous: LinkedSnippetImpl<T>?) : LinkedSnippet<T> {
    override fun get(): T = _val
}

fun <T> LinkedSnippetImpl<T>?.add(value: T) = LinkedSnippetImpl(value, this)
