/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license 
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.util

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.script.experimental.api.KotlinType

open class PropertiesCollection(private val properties: Map<Key<*>, Any> = emptyMap()) {

    data class Key<T>(val name: String, val defaultValue: T? = null)

    class PropertyKeyDelegate<T>(private val defaultValue: T? = null) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Key<T> =
            Key(property.name, defaultValue)
    }

    class PropertyKeyCopyDelegate<T>(val source: Key<T>) {
        operator fun getValue(thisRef: Any?, property: KProperty<*>): Key<T> = source
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: PropertiesCollection.Key<T>): T? =
        properties[key]?.let { it as T } ?: key.defaultValue

    @Suppress("UNCHECKED_CAST")
    fun <T> getNoDefault(key: PropertiesCollection.Key<T>): T? =
        properties[key]?.let { it as T }

    companion object {
        fun <T> key(defaultValue: T? = null) = PropertyKeyDelegate(defaultValue)
        fun <T> keyCopy(source: Key<T>) = PropertyKeyCopyDelegate(source)
    }

    // properties builder base class (DSL for building properties collection)

    open class Builder(baseProperties: Iterable<PropertiesCollection> = emptyList()) {

        val data: MutableMap<PropertiesCollection.Key<*>, Any> = LinkedHashMap<PropertiesCollection.Key<*>, Any>().apply {
            baseProperties.forEach { putAll(it.properties) }
        }

        // generic for all properties

        operator fun <T : Any> PropertiesCollection.Key<T>.invoke(v: T) {
            data[this] = v
        }

        // generic for lists

        operator fun <T> PropertiesCollection.Key<in List<T>>.invoke(vararg vals: T) {
            data[this] = vals.toList()
        }

        // generic for maps:

        operator fun <K, V> PropertiesCollection.Key<Map<K, V>>.invoke(vararg vs: Pair<K, V>) {
            append(vs.asIterable())
        }

        // for strings and list of strings that could be converted from other types

        @JvmName("invoke_string_fqn_from_reflected_class")
        operator fun PropertiesCollection.Key<String>.invoke(kclass: KClass<*>) {
            data[this] = kclass.java.name
        }

        @JvmName("invoke_string_list_fqn_from_reflected_class")
        operator fun PropertiesCollection.Key<in List<String>>.invoke(vararg kclasses: KClass<*>) {
            append(kclasses.map { it.java.name })
        }

        // for KotlinType:

        operator fun PropertiesCollection.Key<KotlinType>.invoke(kclass: KClass<*>) {
            data[this] = KotlinType(kclass)
        }

        operator fun PropertiesCollection.Key<KotlinType>.invoke(ktype: KType) {
            data[this] = KotlinType(ktype)
        }

        operator fun PropertiesCollection.Key<KotlinType>.invoke(fqname: String) {
            data[this] = KotlinType(fqname)
        }

        // for list of KotlinTypes

        operator fun PropertiesCollection.Key<List<KotlinType>>.invoke(vararg classes: KClass<*>) {
            append(classes.map { KotlinType(it) })
        }

        operator fun PropertiesCollection.Key<List<KotlinType>>.invoke(vararg types: KType) {
            append(types.map { KotlinType(it) })
        }

        operator fun PropertiesCollection.Key<List<KotlinType>>.invoke(vararg fqnames: String) {
            append(fqnames.map { KotlinType(it) })
        }

        // for map of generic keys to KotlinTypes:

        @JvmName("invoke_kotlintype_map_from_kclass")
        operator fun <K> PropertiesCollection.Key<Map<K, KotlinType>>.invoke(vararg classes: Pair<K, KClass<*>>) {
            append(classes.map { (k, v) -> k to KotlinType(v) })
        }

        @JvmName("invoke_kotlintype_map_from_ktype")
        operator fun <K> PropertiesCollection.Key<Map<K, KotlinType>>.invoke(vararg types: Pair<K, KType>) {
            append(types.map { (k, v) -> k to KotlinType(v) })
        }

        @JvmName("invoke_kotlintype_map_from_fqname")
        operator fun <K> PropertiesCollection.Key<Map<K, KotlinType>>.invoke(vararg fqnames: Pair<K, String>) {
            append(fqnames.map { (k, v) -> k to KotlinType(v) })
        }

        // direct manipulation - public - for usage in inline dsl methods and for extending dsl

        operator fun <T : Any> set(key: PropertiesCollection.Key<in T>, value: T) {
            data[key] = value
        }

        @Suppress("UNCHECKED_CAST")
        operator fun <T : Any> get(key: PropertiesCollection.Key<in T>): T? = data[key]?.let { it as T }

        operator fun <T : Any> PropertiesCollection.Key<T>.invoke(): T? = get(this)

        // appenders to list and map properties

        @JvmName("appendToList")
        fun <V> PropertiesCollection.Key<in List<V>>.append(values: Iterable<V>) {
            val newValues = get(this)?.let { it + values } ?: values.toList()
            data[this] = newValues
        }

        fun <V> PropertiesCollection.Key<in List<V>>.append(vararg values: V) {
            val newValues = get(this)?.let { it + values } ?: values.toList()
            data[this] = newValues
        }

        fun <K, V> PropertiesCollection.Key<in Map<K, V>>.append(values: Map<K, V>) {
            val newValues = get(this)?.let { it + values } ?: values
            data[this] = newValues
        }

        @JvmName("appendToMap")
        fun <K, V> PropertiesCollection.Key<in Map<K, V>>.append(values: Iterable<Pair<K, V>>) {
            val newValues = get(this)?.let { it + values } ?: values.toMap()
            data[this] = newValues
        }

        // include another builder
        operator fun <T : Builder> T.invoke(body: T.() -> Unit) {
            this.body()
            this@Builder.data.putAll(this.data)
        }
    }
}

fun <T> PropertiesCollection.getOrError(key: PropertiesCollection.Key<T>): T =
    get(key) ?: throw IllegalArgumentException("Unknown key $key")

