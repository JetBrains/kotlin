@file:kotlin.jvm.JvmName("MapAccessorsKt")
package kotlin.properties

import java.io.Serializable
import java.util.NoSuchElementException
import kotlin.platform.platformName

// extensions for Map and MutableMap

/**
 * Returns the value of the property for the given object from this read-only map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and lookup the value corresponding to this name in the map.
 * @return the property value.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the property name and doesn't provide an implicit default (see [withDefault]).
 */
public fun <V> Map<in String, *>.get(thisRef: Any?, property: PropertyMetadata): V = getOrImplicitDefault(property.name) as V

/**
 * Returns the value of the property for the given object from this mutable map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and lookup the value corresponding to this name in the map.
 * @return the property value.
 *
 * @throws NoSuchElementException when the map doesn't contain value for the property name and doesn't provide an implicit default (see [withDefault]).
 */
@platformName("getVar")
public fun <V> MutableMap<in String, in V>.get(thisRef: Any?, property: PropertyMetadata): V = getOrImplicitDefault(property.name) as V

/**
 * Stores the value of the property for the given object in this mutable map.
 * @param thisRef the object for which the value is requested (not used).
 * @param property the metadata for the property, used to get the name of property and store the value associated with that name in the map.
 * @param value the value to set.
 */
public fun <V> MutableMap<in String, in V>.set(thisRef: Any?, property: PropertyMetadata, value: V) {
    this.put(property.name, value)
}
