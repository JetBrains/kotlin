/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import java.io.Serializable

interface Extras {
    class Id<T : Any> constructor(
        val type: ReifiedTypeSignature<T>,
        val name: String? = null,
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Id<*>) return false
            if (name != other.name) return false
            if (type != other.type) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + type.hashCode()
            return result
        }

        override fun toString(): String {
            if (name == null) return type.toString()
            return "$name: $type"
        }

        internal companion object {
            private const val serialVersionUID = 0L
        }
    }

    class Key<T : Any>(
        val id: Id<T>, val capabilities: Set<Capability<T>> = emptySet()
    ) {

        interface Capability<T>

        override fun equals(other: Any?): Boolean {
            if (other === this) return true
            if (other !is Key<*>) return false
            if (other.id != this.id) return false
            if (other.capabilities != this.capabilities) return false
            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + capabilities.hashCode()
            return result
        }

        override fun toString(): String = "Key($id)"

        fun withCapability(capability: Capability<T>): Key<T> {
            return Key(id = id, capabilities + capability)
        }

        inline fun <reified C : Capability<T>> capability(): C? {
            return capabilities.lastOrNull { capability -> capability is C }?.let { it as C }
        }
    }

    class Entry<T : Any>(val key: Key<T>, val value: T) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Entry<*>) return false
            if (other.key != key) return false
            if (other.value != value) return false
            return true
        }

        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + value.hashCode()
            return result
        }

        operator fun component1() = key
        operator fun component2() = value
    }

    val ids: Set<Id<*>>
    operator fun <T : Any> get(key: Key<T>): T?
    operator fun <T : Any> contains(id: Id<T>): Boolean
}

interface IterableExtras : Extras, Iterable<Extras.Entry<*>> {
    val entries: Set<Extras.Entry<*>>
    fun isEmpty() = entries.isEmpty()
    fun isNotEmpty() = !isEmpty()
    override fun iterator(): Iterator<Extras.Entry<*>> = entries.iterator()
}


interface MutableExtras : IterableExtras {
    /**
     * @param value: The new value, or null if the value shall be removed
     * @return The previous value or null if no previous value was set
     */
    operator fun <T : Any> set(key: Extras.Key<T>, value: T?): T?
}

interface HasExtras {
    val extras: Extras
}

interface HasMutableExtras {
    val extras: MutableExtras
}

/**
 * @return the previously set value, when present
 */
fun <T : Any> MutableExtras.remove(key: Extras.Key<T>): T? {
    return set(key, null)
}
