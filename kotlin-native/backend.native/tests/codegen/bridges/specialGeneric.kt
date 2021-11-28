/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.bridges.specialGeneric

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

@Test
fun testMySet() {
    val set = set()
    assertFalse(set.contains(Any()))
    assertFalse(set.contains(NotContainedElement))
    assertTrue(set.contains(ContainedElement))
}
