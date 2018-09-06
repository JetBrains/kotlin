/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

@file:kotlin.jvm.JvmName("MapAccessorsKt")

package kotlin.collections

import kotlin.reflect.KProperty
import kotlin.internal.Exact

/**
 * Returns the value of the property for the given object from this read-only map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and lookup the value corresponding to this name in the map.
 * @return the property value.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the property name and doesn't provide an implicit default (see [withDefault]).
 */
@kotlin.internal.InlineOnly
public inline operator fun <V, V1 : V> Map<in String, @Exact V>.getValue(thisRef: Any?, property: KProperty<*>): V1 =
    @Suppress("UNCHECKED_CAST") (getOrImplicitDefault(property.name) as V1)

/**
 * Returns the value of the property for the given object from this mutable map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and lookup the value corresponding to this name in the map.
 * @return the property value.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the property name and doesn't provide an implicit default (see [withDefault]).
 */
@kotlin.jvm.JvmName("getVar")
@kotlin.internal.InlineOnly
public inline operator fun <V, V1 : V> MutableMap<in String, out @Exact V>.getValue(thisRef: Any?, property: KProperty<*>): V1 =
    @Suppress("UNCHECKED_CAST") (getOrImplicitDefault(property.name) as V1)

@Deprecated("Use getValue() with two type parameters instead", level = DeprecationLevel.ERROR)
@kotlin.jvm.JvmName("getVarContravariant")
@kotlin.internal.LowPriorityInOverloadResolution
@kotlin.internal.InlineOnly
public inline fun <V> MutableMap<in String, in V>.getValue(thisRef: Any?, property: KProperty<*>): V =
    @Suppress("UNCHECKED_CAST") (getOrImplicitDefault(property.name) as V)

/**
 * Stores the value of the property for the given object in this mutable map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and store the value associated with that name in the map.
 * @param value the value to set.
 */
@kotlin.internal.InlineOnly
public inline operator fun <V> MutableMap<in String, in V>.setValue(thisRef: Any?, property: KProperty<*>, value: V) {
    this.put(property.name, value)
}
