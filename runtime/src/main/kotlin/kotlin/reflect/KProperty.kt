/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlin.reflect

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param R the type of the property.
 */
@FixmeReflection
public interface KProperty<out R> : KCallable<R> {
//    /**
//     * `true` if this property is `lateinit`.
//     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/properties.html#late-initialized-properties)
//     * for more information.
//     */
//    @SinceKotlin("1.1")
//    public val isLateinit: Boolean
//
//    /**
//     * `true` if this property is `const`.
//     * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/properties.html#compile-time-constants)
//     * for more information.
//     */
//    @SinceKotlin("1.1")
//    public val isConst: Boolean
//
//    /** The getter of this property, used to obtain the value of the property. */
//    public val getter: Getter<R>
//
//    /**
//     * Represents a property accessor, which is a `get` or `set` method declared alongside the property.
//     * See the [Kotlin language documentation](http://kotlinlang.org/docs/reference/properties.html#getters-and-setters)
//     * for more information.
//     *
//     * @param R the type of the property, which it is an accessor of.
//     */
//    public interface Accessor<out R> {
//        /** The property which this accessor is originated from. */
//        public val property: KProperty<R>
//    }
//
//    /**
//     * Getter of the property is a `get` method declared alongside the property.
//     */
//    public interface Getter<out R> : Accessor<R>, KFunction<R>
}

//
@FixmeReflection
public interface KProperty0<out R> : kotlin.reflect.KProperty<R>, () -> R {
//    public abstract val getter: kotlin.reflect.KProperty1.Getter<T, R>

    public abstract fun get(): R
    public override abstract operator fun invoke(): R

//    @kotlin.SinceKotlin public abstract fun getDelegate(receiver: T): kotlin.Any?
//
//    public interface Getter<T, out R> : kotlin.reflect.KProperty.Getter<R>, (T) -> R {
//    }
}

@FixmeReflection
public interface KProperty1<T, out R> : kotlin.reflect.KProperty<R>, (T) -> R {
//    public abstract val getter: kotlin.reflect.KProperty1.Getter<T, R>

    public abstract fun get(receiver: T): R
    public override abstract operator fun invoke(receiver: T): R

//    @kotlin.SinceKotlin public abstract fun getDelegate(receiver: T): kotlin.Any?
//
//    public interface Getter<T, out R> : kotlin.reflect.KProperty.Getter<R>, (T) -> R {
//    }
}

@FixmeReflection
public interface KProperty2<T1, T2, out R> : kotlin.reflect.KProperty<R>, (T1, T2) -> R {
//    public abstract val getter: kotlin.reflect.KProperty1.Getter<T, R>

    public abstract fun get(receiver1: T1, receiver2: T2): R
    public override abstract operator fun invoke(receiver1: T1, receiver2: T2): R

//    @kotlin.SinceKotlin public abstract fun getDelegate(receiver: T): kotlin.Any?
//
//    public interface Getter<T, out R> : kotlin.reflect.KProperty.Getter<R>, (T) -> R {
//    }
}

/**
 * Represents a property declared as a `var`.
 */
@FixmeReflection
public interface KMutableProperty<R> : KProperty<R> {
//    /** The setter of this mutable property, used to change the value of the property. */
//    public val setter: Setter<R>
//
//    /**
//     * Setter of the property is a `set` method declared alongside the property.
//     */
//    public interface Setter<R> : KProperty.Accessor<R>, KFunction<Unit>
}

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