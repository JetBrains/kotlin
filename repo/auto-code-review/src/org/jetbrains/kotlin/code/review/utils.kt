/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.code.review

fun <T> ArrayDeque<T>.removeFirstUntil(predicate: (T) -> Boolean): List<T> = buildList {
    val deque = this@removeFirstUntil
    while (deque.isNotEmpty() && !predicate(deque.first())) {
        add(deque.removeFirst())
    }
}
