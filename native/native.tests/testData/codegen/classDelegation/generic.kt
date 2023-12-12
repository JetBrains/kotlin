/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class Content() {
    override fun toString() = "OK"
}

interface Box<E> {
    fun get(): E
}

interface ContentBox<T : Content> : Box<T>

object Impl : ContentBox<Content> {
    override fun get(): Content = Content()
}

class ContentBoxDelegate<T : Content>() : ContentBox<T> by (Impl as ContentBox<T>)

fun box() = ContentBoxDelegate<Content>().get().toString()
