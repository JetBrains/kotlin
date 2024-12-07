/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.tooling.core

import java.util.*
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

interface ExtrasProperty<T> {
    val key: Extras.Key<T>
}

val <T : Any> Extras.Key<T>.readWriteProperty get() = extrasReadWriteProperty(this)

fun <Receiver : HasMutableExtras, T> Extras.Key<T>.lazyProperty(factory: Receiver.() -> T) = extrasLazyProperty(this, factory)

fun <T : Any> extrasReadWriteProperty(key: Extras.Key<T>): ExtrasReadWriteProperty<T> = object : ExtrasReadWriteProperty<T> {
    override val key: Extras.Key<T> = key
}

fun <Receiver : HasMutableExtras, T> extrasLazyProperty(
    key: Extras.Key<T>, factory: Receiver.() -> T,
): ExtrasLazyProperty<Receiver, T> =
    object : ExtrasLazyProperty<Receiver, T> {
        override val key: Extras.Key<T> = key
        override val factory: Receiver.() -> T = factory
    }


inline fun <reified T : Any> extrasReadWriteProperty(name: String? = null) =
    extrasReadWriteProperty(extrasKeyOf<T>(name))


inline fun <Receiver : HasMutableExtras, reified T> extrasLazyProperty(name: String? = null, noinline factory: Receiver.() -> T) =
    extrasLazyProperty(extrasKeyOf(name), factory)


interface NotNullExtrasReadOnlyProperty<T : Any> : ExtrasProperty<T>, ReadOnlyProperty<HasExtras, T> {
    val defaultValue: T

    override fun getValue(thisRef: HasExtras, property: KProperty<*>): T {
        return thisRef.extras[key] ?: defaultValue
    }
}

interface ExtrasReadWriteProperty<T : Any> : ExtrasProperty<T>, ReadWriteProperty<HasMutableExtras, T?> {
    override fun getValue(thisRef: HasMutableExtras, property: KProperty<*>): T? {
        return thisRef.extras[key]
    }

    override fun setValue(thisRef: HasMutableExtras, property: KProperty<*>, value: T?) {
        if (value != null) thisRef.extras[key] = value
        else thisRef.extras.remove(key)
    }

    fun notNull(defaultValue: T): NotNullExtrasReadWriteProperty<T> = object : NotNullExtrasReadWriteProperty<T> {
        override val defaultValue: T = defaultValue
        override val key: Extras.Key<T> = this@ExtrasReadWriteProperty.key
    }
}

interface NotNullExtrasReadWriteProperty<T : Any> : ExtrasProperty<T>, ReadWriteProperty<HasMutableExtras, T> {
    val defaultValue: T

    override fun getValue(thisRef: HasMutableExtras, property: KProperty<*>): T {
        return thisRef.extras[key] ?: defaultValue
    }

    override fun setValue(thisRef: HasMutableExtras, property: KProperty<*>, value: T) {
        thisRef.extras[key] = value
    }
}

interface ExtrasLazyProperty<Receiver : HasMutableExtras, T> : ExtrasProperty<T>, ReadWriteProperty<Receiver, T> {
    val factory: Receiver.() -> T

    override fun getValue(thisRef: Receiver, property: KProperty<*>): T {
        return thisRef.extras.getOrPutNullable(key) { thisRef.factory() }
    }

    override fun setValue(thisRef: Receiver, property: KProperty<*>, value: T) {
        thisRef.extras[key] = value
    }
}

/*
DEPRECATED APIs
 */

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
val <T : Any> Extras.Key<T>.readProperty get() = extrasReadProperty(this)

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
fun <T : Any> Extras.Key<T>.factoryProperty(factory: () -> T) = extrasFactoryProperty(this, factory)

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
fun <Receiver : HasMutableExtras, T : Any> Extras.Key<Optional<T>>.nullableLazyProperty(factory: Receiver.() -> T?) =
    extrasNullableLazyProperty(this, factory)

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
fun <T : Any> extrasReadProperty(key: Extras.Key<T>): ExtrasReadOnlyProperty<T> = object : ExtrasReadOnlyProperty<T> {
    override val key: Extras.Key<T> = key
}

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
fun <T : Any> extrasFactoryProperty(key: Extras.Key<T>, factory: () -> T) = object : ExtrasFactoryProperty<T> {
    override val key: Extras.Key<T> = key
    override val factory: () -> T = factory
}

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION")
fun <Receiver : HasMutableExtras, T : Any> extrasNullableLazyProperty(
    key: Extras.Key<Optional<T>>, factory: Receiver.() -> T?,
): NullableExtrasLazyProperty<Receiver, T> =
    object : NullableExtrasLazyProperty<Receiver, T> {
        override val key: Extras.Key<Optional<T>> = key
        override val factory: Receiver.() -> T? = factory
    }

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION", "UNUSED")
inline fun <reified T : Any> extrasReadProperty(name: String? = null) =
    extrasReadProperty(extrasKeyOf<T>(name))


@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION", "UNUSED")
inline fun <reified T : Any> extrasFactoryProperty(name: String? = null, noinline factory: () -> T) =
    extrasFactoryProperty(extrasKeyOf(name), factory)

@Deprecated("Scheduled for removal in Kotlin 2.2")
@Suppress("DEPRECATION", "DeprecatedCallableAddReplaceWith")
inline fun <Receiver : HasMutableExtras, reified T : Any> extrasNullableLazyProperty(
    name: String? = null, noinline factory: Receiver.() -> T?,
) = extrasNullableLazyProperty(extrasKeyOf(name), factory)

@Deprecated("Scheduled for removal in Kotlin 2.2")
interface ExtrasReadOnlyProperty<T : Any> : ExtrasProperty<T>, ReadOnlyProperty<HasExtras, T?> {
    override fun getValue(thisRef: HasExtras, property: KProperty<*>): T? {
        return thisRef.extras[key]
    }

    fun notNull(defaultValue: T): NotNullExtrasReadOnlyProperty<T> = object : NotNullExtrasReadOnlyProperty<T> {
        override val defaultValue: T = defaultValue
        override val key: Extras.Key<T> = this@ExtrasReadOnlyProperty.key
    }
}

@Deprecated("Scheduled for removal in Kotlin 2.2")
interface ExtrasFactoryProperty<T : Any> : ExtrasProperty<T>, ReadWriteProperty<HasMutableExtras, T> {
    val factory: () -> T

    override fun getValue(thisRef: HasMutableExtras, property: KProperty<*>): T {
        return thisRef.extras.getOrPut(key, factory)
    }

    override fun setValue(thisRef: HasMutableExtras, property: KProperty<*>, value: T) {
        thisRef.extras[key] = value
    }
}

@Deprecated("Scheduled for removal in Kotlin 2.2")
interface NullableExtrasLazyProperty<Receiver : HasMutableExtras, T : Any> : ExtrasProperty<Optional<T>>, ReadOnlyProperty<Receiver, T?> {
    val factory: Receiver.() -> T?

    override fun getValue(thisRef: Receiver, property: KProperty<*>): T? {
        return thisRef.extras.getOrPut(key) { Optional.ofNullable(thisRef.factory()) }.let { if (it.isPresent) it.get() else null }
    }
}
