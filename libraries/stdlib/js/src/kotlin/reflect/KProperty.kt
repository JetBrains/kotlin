/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("IMPLEMENTING_FUNCTION_INTERFACE")
package kotlin.reflect

/**
 * Represents a property, such as a named `val` or `var` declaration.
 * Instances of this class are obtainable by the `::` operator.
 * 
 * See the [Kotlin language documentation](https://kotlinlang.org/docs/reference/reflection.html)
 * for more information.
 *
 * @param R the type of the property.
 */
public actual interface KProperty<out R> : KCallable<R>

/**
 * Represents a property declared as a `var`.
 */
public actual interface KMutableProperty<R> : KProperty<R>


/**
 * Represents a property without any kind of receiver.
 * Such property is either originally declared in a receiverless context such as a package,
 * or has the receiver bound to it.
 */
public actual interface KProperty0<out R> : KProperty<R>, () -> R {
    /**
     * Returns the current value of the property.
     */
    public actual fun get(): R
}

/**
 * Represents a `var`-property without any kind of receiver.
 */
public actual interface KMutableProperty0<R> : KProperty0<R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(value: R)
}


/**
 * Represents a property, operations on which take one receiver as a parameter.
 *
 * @param T the type of the receiver which should be used to obtain the value of the property.
 * @param R the type of the property.
 */
public actual interface KProperty1<T, out R> : KProperty<R>, (T) -> R {
    /**
     * Returns the current value of the property.
     *
     * @param receiver the receiver which is used to obtain the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     */
    public actual fun get(receiver: T): R
}

/**
 * Represents a `var`-property, operations on which take one receiver as a parameter.
 */
public actual interface KMutableProperty1<T, R> : KProperty1<T, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver the receiver which is used to modify the value of the property.
     *                 For example, it should be a class instance if this is a member property of that class,
     *                 or an extension receiver if this is a top level extension property.
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(receiver: T, value: R)
}


/**
 * Represents a property, operations on which take two receivers as parameters,
 * such as an extension property declared in a class.
 *
 * @param D the type of the first receiver. In case of the extension property in a class this is
 *        the type of the declaring class of the property, or any subclass of that class.
 * @param E the type of the second receiver. In case of the extension property in a class this is
 *        the type of the extension receiver.
 * @param R the type of the property.
 */
public actual interface KProperty2<D, E, out R> : KProperty<R>, (D, E) -> R {
    /**
     * Returns the current value of the property. In case of the extension property in a class,
     * the instance of the class should be passed first and the instance of the extension receiver second.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     */
    public actual fun get(receiver1: D, receiver2: E): R
}

/**
 * Represents a `var`-property, operations on which take two receivers as parameters.
 */
public actual interface KMutableProperty2<D, E, R> : KProperty2<D, E, R>, KMutableProperty<R> {
    /**
     * Modifies the value of the property.
     *
     * @param receiver1 the instance of the first receiver.
     * @param receiver2 the instance of the second receiver.
     * @param value the new value to be assigned to this property.
     */
    public actual fun set(receiver1: D, receiver2: E, value: R)
}
