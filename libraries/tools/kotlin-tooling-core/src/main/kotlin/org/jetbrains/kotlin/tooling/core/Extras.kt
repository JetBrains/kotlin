/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.tooling.core

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
 *
 * ## [IterableExtras] vs [Extras]
 * Most factories like [extrasOf] or [mutableExtrasOf] will return [IterableExtras].
 * Such an [IterableExtras] container will have all keys materialized and is capable of
 * iterating through all its values. These implementations will also provide a proper [equals] and [hashCode] functions
 *
 * However, some implementations might not be able to promise this and will only be able to provide
 * a value when the actual/proper key is provided. One example for this case would be container that previously
 * was serialized and got de-serialized, unable to provide the keys without additional [Key.Capability]
 *
 * ## Advanced: Capabilities
 * [Key.Capability]ies can be used to attach additional functionality/breadcrumbs alongside the stored value.
 * A very simple example would be storing something like a "pretty String formatter" with the value:
 *
 * ```kotlin
 * data class Person(val name: String, val age: Int, val gibberish: String)
 *
 * interface PrettyDisplayString<T : Any> : Extras.Key.Capability<T> {
 *     fun prettyString(value: T): String
 * }
 *
 * object PersonPrettyDisplayString : PrettyDisplayString<Person> {
 *     override fun prettyString(value: Person): String {
 *         return "${value.name} (${value.age})"
 *     }
 * }
 *
 * fun <T : Any> announce(entry: Extras.Entry<T>) {
 *     val prettyStringCapability = entry.key.capability<PrettyDisplayString<T>>()
 *     val string = prettyStringCapability?.prettyString(entry.value) ?: entry.value.toString()
 *     println("Found: $string")
 * }
 *
 *
 * fun main() {
 *     val extras = mutableExtrasOf()
 *     extras[extrasKeyOf<Person>("pretty") + PersonPrettyDisplayString] = Person("Sunny Cash", 27, "Hakuna Matata")
 *     extras[extrasKeyOf<Person>("ugly")] = Person("Robbie Rotten", 33, "I hate Sportacus")
 *     extras.forEach { entry -> announce(entry) }
 *
 *     // Prints:
 *     // Found: Sunny Cash (27)
 *     // Found: Person(name="Robbie Rotten", age=33, gibberish="I hate Sportacus")
 * }
 * ```
 *
 * Some implementation of [Extras] might require to provide a certain [Key.Capability] in order to access the data stored in the [Extras].
 * E.g. the stored value might be stored in serialized binary format and accessing its value requires "proof" ([Key.Capability]) of
 * deserialization:
 *
 * ```kotlin
 * interface SerializedExtras : Extras {
 *     interface Deserializer<T: Any> : Extras.Key.Capability<T> {
 *         fun deserialize(value: ByteArray): T
 *     }
 * }
 *
 * object StringDeserializer : SerializedExtras.Deserializer<String> {
 *     override fun deserialize(value: ByteArray): String {
 *         return value.decodeToString()
 *     }
 * }
 *
 * fun useExtras(extras: SerializedExtras) {
 *     val plainKey = extrasKeyOf<String>("name")
 *     val keyWithDeserializer = extrasKeyOf<String>("name") + StringDeserializer
 *     extras[plainKey] // <- null: Can't provide Capability of deserialization
 *     extras[keyWithDeserializer] // <- "some value"  || Can be deserialized!
 * }
 * ```
 *
 * Note: Capabilities are not part of the identity of the stored Extra:
 * Setting a value with a given [Extras.Key] with the same [Extras.Id] (type and name matching), will result in
 * the old value being overwritten!
 */
interface Extras {
    class Id<T : Any> @PublishedApi internal constructor(
        internal val type: ReifiedTypeSignature<T>,
        val name: String? = null,
    ) : Serializable {

        val stableString: String
            get() {
                return if (name == null) type.signature
                else "${type.signature};$name"
            }

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

        override fun toString(): String = stableString

        companion object {
            fun fromString(stableString: String): Id<*> {
                @OptIn(UnsafeApi::class) return if (stableString.contains(';')) {
                    val split = stableString.split(';', limit = 2)
                    Id(ReifiedTypeSignature(split[0]), split[1])
                } else Id(ReifiedTypeSignature(stableString))
            }

            private const val serialVersionUID = 0L
        }
    }

    class Key<T : Any> internal constructor(
        val id: Id<T>, @PublishedApi internal val capabilities: Set<Capability<T>>
    ) {
        constructor(id: Id<T>) : this(id, emptySet())

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

        operator fun plus(capability: Capability<T>) = Key(id = id, capabilities + capability)

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

        override fun toString(): String = "$key=$value"

        operator fun component1() = key
        operator fun component2() = value
    }

    val ids: Set<Id<*>>
    operator fun <T : Any> get(key: Key<T>): T?
}

interface IterableExtras : Extras, Collection<Extras.Entry<*>> {
    val entries: Set<Extras.Entry<*>>
    fun isNotEmpty() = !isEmpty()
    override fun iterator(): Iterator<Extras.Entry<*>> = entries.iterator()
}

interface MutableExtras : IterableExtras {
    /**
     * @return The previous value or null if no previous value was set
     */
    operator fun <T : Any> set(key: Extras.Key<T>, value: T): T?

    /**
     * Removes the value from this container if *and only if* it was stored with this key
     */
    fun <T : Any> remove(key: Extras.Key<T>): T?

    /**
     * Removes the corresponding entry (regardless of which key was used) from this map
     */
    fun <T : Any> remove(id: Extras.Id<T>): Extras.Entry<T>?

    fun putAll(from: Iterable<Extras.Entry<*>>)

    fun clear()
}
