/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.jetbrains.kotlin.tooling.core.Extras.Entry
import org.jetbrains.kotlin.tooling.core.Extras.Key
import java.io.Serializable

@Suppress("unchecked_cast")
internal class MutableExtrasImpl(
    initialEntries: Iterable<Entry<*>> = emptyList(),
) : MutableExtras, AbstractExtras(), Serializable {

    private val extras: MutableMap<Key<*>, Entry<*>> =
        initialEntries.associateByTo(mutableMapOf()) { it.key }

    override val keys: Set<Key<*>>
        get() = extras.keys.toSet()

    override val entries: Set<Entry<*>>
        get() = extras.values.toSet()

    override val size: Int
        get() = extras.size

    override fun isEmpty(): Boolean = extras.isEmpty()

    override fun <T> set(key: Key<T>, value: T): T? {
        return put(Entry(key, value))
    }

    override fun <T> put(entry: Entry<T>): T? {
        return extras.put(entry.key, entry)?.let { it.value as T }
    }

    override fun putAll(from: Iterable<Entry<*>>) {
        this.extras.putAll(from.associateBy { it.key })
    }

    override fun <T> get(key: Key<T>): T? {
        return extras[key]?.let { it.value as T }
    }

    override fun <T> remove(key: Key<T>): T? {
        return extras.remove(key)?.let { it.value as T }
    }

    override fun clear() {
        extras.clear()
    }

    internal companion object {
        private const val serialVersionUID = 0L
    }
}

@Suppress("unchecked_cast")
internal class ImmutableExtrasImpl private constructor(
    private val extras: Map<Key<*>, Entry<*>>,
) : AbstractExtras(), Serializable {
    constructor(extras: Iterable<Entry<*>>) : this(extras.associateBy { it.key })

    constructor(extras: Array<out Entry<*>>) : this(extras.associateBy { it.key })

    override val keys: Set<Key<*>> = extras.keys

    override fun isEmpty(): Boolean = extras.isEmpty()

    override val size: Int = extras.size

    override val entries: Set<Entry<*>> = extras.values.toSet()

    override fun <T> get(key: Key<T>): T? {
        return extras[key]?.let { it.value as T }
    }

    internal companion object {
        private const val serialVersionUID = 0L
    }

    /* Replace during serialization */
    private fun writeReplace(): Any = Surrogate(entries)

    private class Surrogate(private val entries: Set<Entry<*>>) : Serializable {
        fun readResolve(): Any = ImmutableExtrasImpl(entries)

        private companion object {
            private const val serialVersionUID = 0L
        }
    }
}

abstract class AbstractExtras : Extras {

    override val size: Int get() = keys.size

    override fun isEmpty(): Boolean = keys.isEmpty()

    override fun contains(key: Key<*>): Boolean = key in keys

    override fun contains(element: Entry<*>): Boolean =
        entries.contains(element)

    override fun containsAll(elements: Collection<Entry<*>>): Boolean =
        entries.containsAll(elements)

    override fun iterator(): Iterator<Entry<*>> = entries.iterator()

    override fun equals(other: Any?): Boolean {
        if (other === this) return true
        if (other !is Extras) return false
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

internal object EmptyExtras : AbstractExtras(), Serializable {

    override val size: Int = 0

    override val keys: Set<Key<*>> = emptySet()

    override val entries: Set<Entry<*>> = emptySet()

    override fun isEmpty(): Boolean = true

    override fun <T> get(key: Key<T>): T? = null

    override fun contains(key: Key<*>): Boolean = false

    override fun contains(element: Entry<*>): Boolean = false

    @Suppress("unused") // Necessary for java.io.Serializable stability
    private const val serialVersionUID = 0L

    /* Ensure single instance, even after deserialization */
    private fun readResolve(): Any = EmptyExtras
}
