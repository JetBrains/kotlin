/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.properties

import kotlin.reflect.KProperty

/**
 * Base interface that can be used for implementing property delegates of read-only properties.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 * @param V the type of the property value.
 */
public fun interface ReadOnlyProperty<in T, out V> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public operator fun getValue(thisRef: T, property: KProperty<*>): V
}

/**
 * Base interface that can be used for implementing property delegates of read-write properties.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your property delegate has methods with the same signatures.
 *
 * @param T the type of object which owns the delegated property.
 * @param V the type of the property value.
 */
public interface ReadWriteProperty<in T, V> : ReadOnlyProperty<T, V> {
    /**
     * Returns the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @return the property value.
     */
    public override operator fun getValue(thisRef: T, property: KProperty<*>): V

    /**
     * Sets the value of the property for the given object.
     * @param thisRef the object for which the value is requested.
     * @param property the metadata for the property.
     * @param value the value to set.
     */
    public operator fun setValue(thisRef: T, property: KProperty<*>, value: V)
}

/**
 * Base interface that can be used for implementing property delegate providers.
 *
 * This is provided only for convenience; you don't have to extend this interface
 * as long as your delegate provider has a method with the same signature.
 *
 * @param T the type of object which owns the delegated property.
 * @param D the type of property delegates this provider provides.
 */
@SinceKotlin("1.4")
public fun interface PropertyDelegateProvider<in T, out D> {
    /**
     * Returns the delegate of the property for the given object.
     *
     * This function can be used to extend the logic of creating the object (e.g. perform validation checks)
     * to which the property implementation is delegated.
     *
     * @param thisRef the object for which property delegate is requested.
     * @param property the metadata for the property.
     * @return the property delegate.
     */
    public operator fun provideDelegate(thisRef: T, property: KProperty<*>): D
}
