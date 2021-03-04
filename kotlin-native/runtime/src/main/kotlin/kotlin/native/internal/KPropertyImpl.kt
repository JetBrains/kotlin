/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.native.internal

import kotlin.UnsupportedOperationException
import kotlin.reflect.*

@FixmeReflection
open class KProperty0Impl<out R>(override val name: String, override val returnType: KType, val getter: () -> R): KProperty0<R> {
    override fun get(): R {
        return getter()
    }
    override fun invoke(): R {
        return getter()
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KProperty0Impl<*>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

@FixmeReflection
open class KProperty1Impl<T, out R>(override val name: String, override val returnType: KType, val getter: (T) -> R): KProperty1<T, R> {
    override fun get(receiver: T): R {
        return getter(receiver)
    }
    override fun invoke(p1: T): R {
        return getter(p1)
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KProperty1Impl<*, *>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

@FixmeReflection
open class KProperty2Impl<T1, T2, out R>(override val name: String, override val returnType: KType, val getter: (T1, T2) -> R): KProperty2<T1, T2, R> {
    override fun get(receiver1: T1, receiver2: T2): R {
        return getter(receiver1, receiver2)
    }
    override fun invoke(p1: T1, p2: T2): R {
        return getter(p1, p2)
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KProperty2Impl<*, *, *>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

@FixmeReflection
class KMutableProperty0Impl<R>(name: String, returnType: KType, getter: () -> R, val setter: (R) -> Unit)
    : KProperty0Impl<R>(name, returnType, getter), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        setter(value)
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KMutableProperty0Impl<*>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

@FixmeReflection
class KMutableProperty1Impl<T, R>(name: String, returnType: KType, getter: (T) -> R, val setter: (T, R) -> Unit)
    : KProperty1Impl<T, R>(name, returnType, getter), KMutableProperty1<T, R> {
    override fun set(receiver: T, value: R): Unit {
        setter(receiver, value)
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KMutableProperty1Impl<*, *>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

@FixmeReflection
class KMutableProperty2Impl<T1, T2, R>(name: String, returnType: KType, getter: (T1, T2) -> R, val setter: (T1, T2, R) -> Unit)
    : KProperty2Impl<T1, T2, R>(name, returnType, getter), KMutableProperty2<T1, T2, R> {
    override fun set(receiver1: T1, receiver2: T2, value: R): Unit {
        setter(receiver1, receiver2, value)
    }

    override fun equals(other: Any?): Boolean {
        val otherKProperty = other as? KMutableProperty2Impl<* ,*, *>
        if (otherKProperty == null) return false
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

open class KLocalDelegatedPropertyImpl<out R>(override val name: String, override val returnType: KType): KProperty0<R> {
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

class KLocalDelegatedMutablePropertyImpl<R>(name: String, returnType: KType): KLocalDelegatedPropertyImpl<R>(name, returnType), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}