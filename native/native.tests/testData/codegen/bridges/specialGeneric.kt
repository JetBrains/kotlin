/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

interface Element {
    val isContained: Boolean
}

object ContainedElement : Element {
    override val isContained: Boolean = true
}

object NotContainedElement : Element {
    override val isContained: Boolean = false
}

internal class MySet<E : Element> : Set<E> {
    override fun contains(element: E): Boolean = element.isContained

    override fun equals(other: Any?): Boolean = TODO()
    override fun hashCode(): Int = TODO()
    override fun toString(): String = TODO()

    override val size: Int get() = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun containsAll(elements: Collection<E>): Boolean = TODO()

    override fun iterator(): Iterator<E> = TODO()
}

fun set(): Set<Any> = MySet<Element>()

fun box(): String {
    val set = set()
    if (set.contains(Any())) return "FAIL 1: $set"
    if (set.contains(NotContainedElement)) return "FAIL 2: $set"
    if (!set.contains(ContainedElement)) return "FAIL 3: $set"

    return "OK"
}
