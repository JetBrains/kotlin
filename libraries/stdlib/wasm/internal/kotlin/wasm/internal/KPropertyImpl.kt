/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.wasm.internal

import kotlin.UnsupportedOperationException
import kotlin.reflect.*

internal abstract class KPropertyImplBase(private val reflectionTargetLinkageError: String?) {
    protected fun maybeThrowPLError(): Nothing? {
        reflectionTargetLinkageError?.let {
            throwLinkageError(it)
        }
        return null
    }
}

internal abstract class KProperty0ImplBase<out R>(reflectionTargetLinkageError: String?) : KPropertyImplBase(reflectionTargetLinkageError), KProperty0<R> {
    abstract val getter: KFunction0<R>

    override fun get(): R {
        return getter()
    }

    override fun invoke(): R {
        return getter()
    }
}

internal final class KProperty0Impl<out R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction0<R>,
) : KProperty0ImplBase<R>(reflectionTargetLinkageError) {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KProperty0Impl<*>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}

internal abstract class KProperty1ImplBase<T, out R>(reflectionTargetLinkageError: String?) : KPropertyImplBase(reflectionTargetLinkageError), KProperty1<T, R> {
    abstract val getter: KFunction1<T, R>

    override fun get(receiver: T): R {
        return getter(receiver)
    }

    override fun invoke(p1: T): R {
        return getter(p1)
    }
}

internal class KProperty1Impl<T, out R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction1<T, R>,
) : KProperty1ImplBase<T, R>(reflectionTargetLinkageError) {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KProperty1Impl<*, *>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}


internal abstract class KProperty2ImplBase<T1, T2, out R>(reflectionTargetLinkageError: String?) : KPropertyImplBase(reflectionTargetLinkageError), KProperty2<T1, T2, R> {
    abstract val getter: KFunction2<T1, T2, R>

    override fun get(receiver1: T1, receiver2: T2): R {
        return getter(receiver1, receiver2)
    }

    override fun invoke(p1: T1, p2: T2): R {
        return getter(p1, p2)
    }
}

internal class KProperty2Impl<T1, T2, out R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction2<T1, T2, R>,
) : KProperty2ImplBase<T1, T2, R>(reflectionTargetLinkageError) {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KProperty2Impl<*, *, *>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return name.hashCode() * 31 + getter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}


internal class KMutableProperty0Impl<R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction0<R>,
    val setter: (R) -> Unit
) : KProperty0ImplBase<R>(reflectionTargetLinkageError), KMutableProperty0<R> {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun set(value: R): Unit {
        setter(value)
    }

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KMutableProperty0Impl<*>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KMutableProperty1Impl<T, R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction1<T, R>,
    val setter: (T, R) -> Unit,
) : KProperty1ImplBase<T, R>(reflectionTargetLinkageError), KMutableProperty1<T, R> {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun set(receiver: T, value: R): Unit {
        setter(receiver, value)
    }

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KMutableProperty1Impl<*, *>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KMutableProperty2Impl<T1, T2, R>(
    private val _name: String?,
    reflectionTargetLinkageError: String?,
    override val getter: KFunction2<T1, T2, R>,
    val setter: (T1, T2, R) -> Unit,
) : KProperty2ImplBase<T1, T2, R>(reflectionTargetLinkageError), KMutableProperty2<T1, T2, R> {
    override val name: String get() = maybeThrowPLError() ?: _name!!

    override fun set(receiver1: T1, receiver2: T2, value: R): Unit {
        setter(receiver1, receiver2, value)
    }

    override fun equals(other: Any?): Boolean {
        maybeThrowPLError()
        val otherKProperty = other as? KMutableProperty2Impl<*, *, *>
        if (otherKProperty == null) return false
        otherKProperty.maybeThrowPLError()
        return name == otherKProperty.name && getter == otherKProperty.getter && setter == otherKProperty.setter
    }

    override fun hashCode(): Int {
        maybeThrowPLError()
        return (name.hashCode() * 31 + getter.hashCode()) * 31 + setter.hashCode()
    }

    override fun toString(): String {
        maybeThrowPLError()
        return "property $name (Kotlin reflection is not available)"
    }
}

internal abstract class KLocalDelegatedPropertyImplBase<out R> : KProperty0<R> {
    override fun get(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun invoke(): R {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }
}

internal class KLocalDelegatedPropertyImpl<out R>(override val name: String) : KLocalDelegatedPropertyImplBase<R>() {
    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}

internal class KLocalDelegatedMutablePropertyImpl<R>(override val name: String) :
    KLocalDelegatedPropertyImplBase<R>(), KMutableProperty0<R> {
    override fun set(value: R): Unit {
        throw UnsupportedOperationException("Not supported for local property reference.")
    }

    override fun toString(): String {
        return "property $name (Kotlin reflection is not available)"
    }
}