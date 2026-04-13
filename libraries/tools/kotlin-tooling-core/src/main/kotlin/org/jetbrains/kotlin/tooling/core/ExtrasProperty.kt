/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("DeprecatedCallableAddReplaceWith")

package org.jetbrains.kotlin.tooling.core

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
