/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

import org.jetbrains.kotlin.tooling.core.Extras.Entry
import org.jetbrains.kotlin.tooling.core.Extras.Key
import java.io.Serializable

/**
 * A generic container holding typed and scoped values.
 * ### Attaching and getting simple typed values:
 * ```kotlin
 * val extras = mutableExtrasOf()
 * extras[extrasKeyOf<Int>()] = 42 // Attach arbitrary Int value
 * extras[extrasKeyOf<String>()] = "Hello" // Attach arbitrary String value
 *
 * extras[extrasKeyOf<Int>()] // -> returns 42
 * extras[extrasKeyOf<String>] // -> returns "Hello"
 * ```
 *
 * ### Attaching multiple values with the same type by naming the keys
 * ```kotlin
 * val extras = mutableExtrasOf()
 * extras[extrasKeyOf<Int>("a")] = 1 // Attach Int with name 'a'
 * extras[extrasKeyOf<Int>("b")] = 2 // Attach Int with name 'b'
 *
 * extras[extrasKeyOf<Int>("a")] // -> returns 1
 * extras[extrasKeyOf<Int>("b")] // -> returns 2
 * ```
 *
 * ### Creating immutable extras
 * ```kotlin
 * val extras = extrasOf(
 *     extrasKeyOf<Int>() withValue 1,
 *     extrasKeyOf<String>() withValue "Hello"
 * )
 * ```
 *
 * ### Converting to immutable extras
 * ```kotlin
 * val extras = mutableExtrasOf(
 *     extrasKeyOf<Int>() withValue 0
 * )
 *
 * // Captures the content, similar to `.toList()` or `.toSet()`
 * val immutableExtras = extras.toExtras()
 * ```
 *
 * ### Use case example: Filtering Extras
 * ```kotlin
 * val extras = extrasOf(
 *     extrasKeyOf<Int>() withValue 0,
 *     extrasKeyOf<Int>("keep") withValue 1,
 *     extrasKeyOf<String>() withValue "Hello"
 * )
 *
 * val filteredExtras = extras
 *     .filter { (key, value) -> key.id.name == "keep" || value is String }
 *     .toExtras()
 * ```
 */
interface Extras : Collection<Entry<out Any>> {
    class Type<T> @UnsafeApi("Use 'extrasTypeOf()' instead") @PublishedApi internal constructor(
        internal val signature: String
    ) : Serializable {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Type<*>) return false
            if (other.signature != this.signature) return false
            return true
        }

        override fun hashCode(): Int {
            return 31 * signature.hashCode()
        }

        override fun toString(): String = signature

        internal companion object {
            private const val serialVersionUID = 0L
        }
    }

    /* Not implemented as data class to ensure more controllable binary compatibility */
    class Key<T : Any> @PublishedApi internal constructor(
        val type: Type<T>,
        val name: String? = null,
    ) : Serializable {

        val stableString: String
            get() {
                return if (name == null) type.signature
                else "${type.signature};$name"
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Key<*>) return false
            if (name != other.name) return false
            if (type != other.type) return false
            return true
        }

        override fun hashCode(): Int {
            var result = name?.hashCode() ?: 0
            result = 31 * result + type.hashCode()
            return result
        }

        override fun toString(): String = stableString

        companion object {
            fun fromString(stableString: String): Key<*> {
                @OptIn(UnsafeApi::class) return if (stableString.contains(';')) {
                    val split = stableString.split(';', limit = 2)
                    Key(Type(split[0]), split[1])
                } else Key(Type(stableString))
            }

            private const val serialVersionUID = 0L
        }
    }

    /* Not implemented as data class to ensure more controllable binary compatibility */
    class Entry<T : Any>(val key: Key<T>, val value: T) : Serializable {
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

        override fun toString(): String = "$key=$value"

        operator fun component1() = key
        operator fun component2() = value

        internal companion object {
            private const val serialVersionUID = 0L
        }
    }

    val keys: Set<Key<*>>
    val entries: Set<Entry<*>>
    operator fun <T : Any> get(key: Key<T>): T?
    operator fun contains(key: Key<*>): Boolean
    override fun iterator(): Iterator<Entry<out Any>>
}

interface MutableExtras : Extras {
    /**
     * @return The previous value or null if no previous value was set
     */
    operator fun <T : Any> set(key: Key<T>, value: T): T?

    fun <T : Any> put(entry: Entry<T>): T?

    fun putAll(from: Iterable<Entry<*>>)

    fun <T : Any> remove(key: Key<T>): T?

    fun clear()
}
