package org.jetbrains.dokka.utilities

interface SelfRepresentingSingletonSet<T : SelfRepresentingSingletonSet<T>> : Set<T> {
    override val size: Int get() = 1

    override fun contains(element: T): Boolean = this == element

    override fun containsAll(elements: Collection<T>): Boolean =
        if (elements.isEmpty()) true
        else elements.all { this == it }

    override fun isEmpty(): Boolean = false

    override fun iterator(): Iterator<T> = iterator {
        @Suppress("UNCHECKED_CAST")
        yield(this@SelfRepresentingSingletonSet as T)
    }
}
