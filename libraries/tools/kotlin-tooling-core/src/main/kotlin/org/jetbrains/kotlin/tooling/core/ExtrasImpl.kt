/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

@Suppress("unchecked_cast")
internal class MutableExtrasImpl(
    initialEntries: Iterable<Extras.Entry<*>> = emptyList()
) : MutableExtras, AbstractIterableExtras() {

    private val extras: MutableMap<Extras.Id<*>, Extras.Entry<*>> =
        initialEntries.associateByTo(mutableMapOf()) { it.key.id }

    override val ids: Set<Extras.Id<*>>
        get() = extras.keys

    override val entries: Set<Extras.Entry<*>>
        get() = extras.values.toSet()

    override val size: Int
        get() = extras.size

    override fun isEmpty(): Boolean = extras.isEmpty()

    override fun <T : Any> set(key: Extras.Key<T>, value: T): T? {
        return extras.put(key.id, key withValue value)?.let { it.value as T }
    }

    override fun putAll(from: Iterable<Extras.Entry<*>>) {
        this.extras.putAll(from.associateBy { it.key.id })
    }

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key.id]?.let { it.value as T }
    }

    override fun <T : Any> remove(id: Extras.Id<T>): Extras.Entry<T>? {
        return extras.remove(id)?.let { it as Extras.Entry<T> }
    }

    override fun <T : Any> remove(key: Extras.Key<T>): T? {
        val entry = extras[key.id]
        if (entry?.key == key) {
            return remove(key.id)?.value
        }
        return null
    }

    override fun clear() {
        extras.clear()
    }
}

@Suppress("unchecked_cast")
internal class ImmutableExtrasImpl private constructor(
    private val extras: Map<Extras.Id<*>, Extras.Entry<*>>
) : AbstractIterableExtras() {
    constructor(extras: Iterable<Extras.Entry<*>>) : this(extras.associateBy { it.key.id })

    constructor(extras: Array<out Extras.Entry<*>>) : this(extras.associateBy { it.key.id })

    override val ids: Set<Extras.Id<*>> = extras.keys

    override fun isEmpty(): Boolean = extras.isEmpty()

    override val size: Int = extras.size

    override val entries: Set<Extras.Entry<*>> = extras.values.toSet()

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key.id]?.let { it.value as T }
    }
}

abstract class AbstractIterableExtras : IterableExtras {

    override val size: Int get() = entries.size

    override fun isEmpty(): Boolean = entries.isEmpty()

    override fun contains(element: Extras.Entry<*>): Boolean =
        entries.contains(element)

    override fun containsAll(elements: Collection<Extras.Entry<*>>): Boolean =
        entries.containsAll(elements)

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is IterableExtras) return false
        if (other.entries != this.entries) return false
        return true
    }

    override fun hashCode(): Int {
        return 31 * entries.hashCode()
    }

    override fun toString(): String {
        return "Extras($entries)"
    }
}

internal object EmptyExtras : AbstractIterableExtras() {
    override val size: Int = 0
    override val ids: Set<Extras.Id<*>> = emptySet()
    override val entries: Set<Extras.Entry<*>> = emptySet()
    override fun isEmpty(): Boolean = true
    override fun <T : Any> get(key: Extras.Key<T>): T? = null
}
