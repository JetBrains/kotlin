/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IMPLEMENTING_FUNCTION_INTERFACE")

package kotlin.wasm.internal

import kotlin.UnsupportedOperationException
import kotlin.reflect.*

internal open class KProperty0Impl<out R>(
    override val name: String,
    internal val container: String,
    internal val capturedReceiver: Boolean,
    val getter: () -> R,
) : KProperty0<R> {
    override fun get(): R {
        return getter()
    }

    override fun invoke(): R {
        return getter()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KProperty0Impl<*> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter)
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + container.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal open class KProperty1Impl<T, out R>(
    override val name: String,
    internal val container: String,
    internal val capturedReceiver: Boolean,
    val getter: (T) -> R,
) : KProperty1<T, R> {
    override fun get(receiver: T): R {
        return getter(receiver)
    }

    override fun invoke(p1: T): R {
        return getter(p1)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KProperty1Impl<*, *> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter)
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + container.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal open class KProperty2Impl<T1, T2, out R>(
    override val name: String,
    internal val container: String,
    internal val capturedReceiver: Boolean,
    val getter: (T1, T2) -> R,
) :
    KProperty2<T1, T2, R> {
    override fun get(receiver1: T1, receiver2: T2): R {
        return getter(receiver1, receiver2)
    }

    override fun invoke(p1: T1, p2: T2): R {
        return getter(p1, p2)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KProperty2Impl<*, *, *> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter)
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + container.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KMutableProperty0Impl<R>(
    name: String,
    container: String,
    capturedReceiver: Boolean,
    getter: () -> R,
    val setter: (R) -> Unit,
) : KProperty0Impl<R>(name, container, capturedReceiver, getter), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        setter(value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KMutableProperty0Impl<*> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter && setter == otherKProperty.setter)
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + container.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KMutableProperty1Impl<T, R>(
    name: String,
    container: String,
    capturedReceiver: Boolean,
    getter: (T) -> R,
    val setter: (T, R) -> Unit,
) : KProperty1Impl<T, R>(name, container, capturedReceiver, getter), KMutableProperty1<T, R> {
    override fun set(receiver: T, value: R): Unit {
        setter(receiver, value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KMutableProperty1Impl<*, *> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter && setter == otherKProperty.setter)
    }

    override fun hashCode(): Int {
        return (name.hashCode() * 31 + container.hashCode())
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KMutableProperty2Impl<T1, T2, R>(
    name: String,
    container: String,
    capturedReceiver: Boolean,
    getter: (T1, T2) -> R,
    val setter: (T1, T2, R) -> Unit,
) :
    KProperty2Impl<T1, T2, R>(name, container, capturedReceiver, getter), KMutableProperty2<T1, T2, R> {
    override fun set(receiver1: T1, receiver2: T2, value: R): Unit {
        setter(receiver1, receiver2, value)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        val otherKProperty = other as? KMutableProperty2Impl<*, *, *> ?: return false
        return container == otherKProperty.container &&
                name == otherKProperty.name &&
                (!capturedReceiver || getter == otherKProperty.getter && setter == otherKProperty.setter)
    }

    override fun hashCode(): Int {
        return (name.hashCode() * 31 + container.hashCode())
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal open class KLocalDelegatedPropertyImpl<out R>(override val name: String) : KProperty0<R> {
    override fun get(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun invoke(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KLocalDelegatedMutablePropertyImpl<R>(name: String) : KLocalDelegatedPropertyImpl<R>(name), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}