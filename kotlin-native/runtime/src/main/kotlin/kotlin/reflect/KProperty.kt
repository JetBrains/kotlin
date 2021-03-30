/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.reflect

import kotlin.native.internal.FixmeReflection

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 *
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param V the type of the property value.
 */
public actual interface KProperty<out V> : KCallable<V>

public actual interface KProperty0<out V> : kotlin.reflect.KProperty<V>, () -> V {

    public actual fun get(): V

    public override abstract operator fun invoke(): V
}

public actual interface KProperty1<T, out V> : kotlin.reflect.KProperty<V>, (T) -> V {
    public actual fun get(receiver: T): V

    public override operator fun invoke(p1: T): V
}

public actual interface KProperty2<D, E, out V> : kotlin.reflect.KProperty<V>, (D, E) -> V {
    public actual fun get(receiver1: D, receiver2: E): V

    public override operator fun invoke(p1: D, p2: E): V
}

/**
 * Represents a property declared as a `var`.
 */
public actual interface KMutableProperty<V> : KProperty<V>

public actual interface KMutableProperty0<V> : KProperty0<V>, KMutableProperty<V> {
    public actual fun set(value: V)
}

public actual interface KMutableProperty1<T, V> : KProperty1<T, V>, KMutableProperty<V> {
    public actual fun set(receiver: T, value: V)
}

public actual interface KMutableProperty2<D, E, V> : KProperty2<D, E, V>, KMutableProperty<V> {
    public actual fun set(receiver1: D, receiver2: E, value: V)
}