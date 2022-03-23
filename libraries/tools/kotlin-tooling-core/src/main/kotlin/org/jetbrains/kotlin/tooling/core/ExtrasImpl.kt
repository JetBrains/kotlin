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

    override fun isEmpty(): Boolean = extras.isEmpty()

    override fun <T : Any> set(key: Extras.Key<T>, value: T?): T? {
        return if (value == null) extras.remove(key.id)?.let { it.value as T }
        else extras.put(key.id, key withValue value)?.let { it.value as T }
    }

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key.id]?.let { it.value as T }
    }

    override fun <T : Any> contains(id: Extras.Id<T>): Boolean {
        return ids.contains(id)
    }
}

@Suppress("unchecked_cast")
internal class ImmutableExtrasImpl(
    private val extras: Map<Extras.Id<*>, Extras.Entry<*>>
) : AbstractIterableExtras() {
    constructor(extras: Iterable<Extras.Entry<*>>) : this(extras.associateBy { it.key.id })

    override val ids: Set<Extras.Id<*>> = extras.keys

    override fun isEmpty(): Boolean = extras.isEmpty()

    override val entries: Set<Extras.Entry<*>> = extras.values.toSet()

    override fun <T : Any> get(key: Extras.Key<T>): T? {
        return extras[key.id]?.let { it.value as T }
    }

    override fun <T : Any> contains(id: Extras.Id<T>): Boolean = id in ids
}

abstract class AbstractIterableExtras : IterableExtras {
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
        return "Extras(${entries.joinToString(", ") { "${it.key}:${it.value}" }})"
    }
}

internal object EmptyExtras : AbstractIterableExtras() {
    override val ids: Set<Extras.Id<*>> = emptySet()
    override val entries: Set<Extras.Entry<*>> = emptySet()
    override fun <T : Any> get(key: Extras.Key<T>): T? = null
    override fun <T : Any> contains(id: Extras.Id<T>): Boolean = false
}
