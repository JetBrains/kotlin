/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package kotlin.reflect

import kotlin.native.internal.FixmeReflection

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param R the type of the property.
 */
@FixmeReflection
public interface KProperty<out R> : KCallable<R>

@FixmeReflection
public interface KProperty0<out R> : kotlin.reflect.KProperty<R>, () -> R {

    public abstract fun get(): R

    public override abstract operator fun invoke(): R
}

@FixmeReflection
public interface KProperty1<T, out R> : kotlin.reflect.KProperty<R>, (T) -> R {
    public abstract fun get(p1: T): R

    public override abstract operator fun invoke(p1: T): R
}

@FixmeReflection
public interface KProperty2<T1, T2, out R> : kotlin.reflect.KProperty<R>, (T1, T2) -> R {
    public abstract fun get(p1: T1, p2: T2): R

    public override abstract operator fun invoke(p1: T1, p2: T2): R
}

/**
 * Represents a property declared as a `var`.
 */
@FixmeReflection
public interface KMutableProperty<R> : KProperty<R>

@FixmeReflection
public interface KMutableProperty0<R> : KProperty0<R>, KMutableProperty<R> {
    public abstract fun set(value: R)
}

@FixmeReflection
public interface KMutableProperty1<T, R> : KProperty1<T, R>, KMutableProperty<R> {
    public abstract fun set(receiver: T, value: R)
}

@FixmeReflection
public interface KMutableProperty2<T1, T2, R> : KProperty2<T1, T2, R>, KMutableProperty<R> {
    public abstract fun set(receiver1: T1, receiver2: T2, value: R)
}