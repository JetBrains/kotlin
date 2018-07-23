/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.script.experimental.util.ChainedPropertyBag
import kotlin.script.experimental.util.TypedKey

interface PropertiesGroup


open class PropertiesBuilder(val props: ScriptingProperties)

class PropertiesBuilderDelegate<T: PropertiesBuilder>(val kclass: KClass<T>) {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): KClass<T> = kclass
}

inline fun <reified T : PropertiesBuilder> propertiesBuilder() = PropertiesBuilderDelegate(T::class)


open class ScriptingProperties(body: ScriptingProperties.() -> Unit = {}) {

    var parentPropertiesBag: ChainedPropertyBag? = null
    var parentPropertiesBuilder: ScriptingProperties? = null
    val data = HashMap<TypedKey<*>, Any?>()

    init {
        body()
    }

    open fun setup() {}

    internal fun makePropertyBag(): ChainedPropertyBag =
        ChainedPropertyBag.createOptimized(parentPropertiesBag ?: parentPropertiesBuilder?.makePropertyBag(), data)

    // --------------------------
    // DSL:

    // generic invoke for properties groups, allowing to use syntax:
    //   PropertiesGroup {
    //     ...
    //   }

    inline operator fun <T : PropertiesGroup> T.invoke(body: T.() -> Unit) = body()

    // generic invoke for properties builder - for extending dsl with complex builders

    inline operator fun <reified T : PropertiesBuilder> KClass<T>.invoke(body: T.() -> Unit) {
        constructors.first().call(this@ScriptingProperties).body()
    }

    // chaining:

    private fun chain(propsBag: ChainedPropertyBag?, propsBuilder: ScriptingProperties?, replaceParent: Boolean) {
        assert(propsBag == null || propsBuilder == null)
        if (!replaceParent && parentPropertiesBag != null || parentPropertiesBuilder != null)
            throw RuntimeException("Parent already defined for properties being build: ${parentPropertiesBag ?: parentPropertiesBuilder}")
        parentPropertiesBag = propsBag
        parentPropertiesBuilder = propsBuilder
    }

    fun chain(props: ScriptingProperties, replaceParent: Boolean = false) {
        chain(null, props, replaceParent)
    }

    fun chain(props: ChainedPropertyBag, replaceParent: Boolean = false) {
        chain(props, null, replaceParent)
    }

    // inclusion:

    fun include(props: ScriptingProperties) {
        data.putAll(props.data)
    }

    fun include(props: ChainedPropertyBag) {
        data.putAll(props.data)
    }

    // builders for known property types, allowing to use syntax
    //   propertyKey(value...)
    // or
    //   propertyKey<Type>()

    // generic:

    inline operator fun <reified T> TypedKey<T>.invoke(v: T) {
        data[this] = v
    }

    // for KotlinType:

    inline operator fun <reified K> TypedKey<KotlinType>.invoke() {
        data[this] = KotlinType(K::class)
    }

    operator fun TypedKey<KotlinType>.invoke(kclass: KClass<*>) {
        data[this] = KotlinType(kclass)
    }

    operator fun TypedKey<KotlinType>.invoke(ktype: KType) {
        data[this] = KotlinType(ktype)
    }

    operator fun TypedKey<KotlinType>.invoke(fqname: String) {
        data[this] = KotlinType(fqname)
    }

    // for list of KotlinTypes

    @JvmName("invoke_kotlintype_list_from_generic")
    inline operator fun <reified K> TypedKey<in List<KotlinType>>.invoke() {
        data.addToListProperty(this, KotlinType(K::class))
    }

    operator fun TypedKey<List<KotlinType>>.invoke(vararg classes: KClass<*>) {
        data.addToListProperty(this, classes.map { KotlinType(it) })
    }

    operator fun TypedKey<List<KotlinType>>.invoke(vararg types: KType) {
        data.addToListProperty(this, types.map { KotlinType(it) })
    }

    operator fun TypedKey<List<KotlinType>>.invoke(vararg fqnames: String) {
        data.addToListProperty(this, fqnames.map { KotlinType(it) })
    }

    // generic for list

    inline operator fun <reified E> TypedKey<List<E>>.invoke(vararg vs: E) {
        data.addToListProperty(this, vs.toList())
    }

    // for map of generic keys to KotlinTypes:

    @JvmName("invoke_kotlintype_map_from_kclass")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg classes: Pair<K, KClass<*>>) {
        data.addToMapProperty(this, classes.map { (k, v) -> k to KotlinType(v) })
    }

    @JvmName("invoke_kotlintype_map_from_ktype")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg types: Pair<K, KType>) {
        data.addToMapProperty(this, types.map { (k, v) -> k to KotlinType(v) })
    }

    @JvmName("invoke_kotlintype_map_from_fqname")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg fqnames: Pair<K, String>) {
        data.addToMapProperty(this, fqnames.map { (k, v) -> k to KotlinType(v) })
    }

    // generic for maps:

    inline operator fun <reified K, reified V> TypedKey<Map<K, V>>.invoke(vararg vs: Pair<K, V>) {
        data.addToMapProperty(this, vs.asIterable())
    }

    // for strings and list of strings that could be converted from other types

    @JvmName("invoke_string_fqn_from_generic")
    inline operator fun <reified K> TypedKey<String>.invoke() {
        data[this] = K::class.qualifiedName!!
    }

    @JvmName("invoke_string_fqn_from_reflected_class")
    operator fun TypedKey<String>.invoke(kclass: KClass<*>) {
        data[this] = kclass.qualifiedName!!
    }

    @JvmName("invoke_string_list_fqn_from_generic")
    inline operator fun <reified K> TypedKey<in List<String>>.invoke() {
        data.addToListProperty(this, K::class.qualifiedName!!)
    }

    @JvmName("invoke_string_list_fqn_from_reflected_class")
    operator fun TypedKey<in List<String>>.invoke(vararg kclasses: KClass<*>) {
        data.addToListProperty(this, kclasses.map { it.qualifiedName!! })
    }
}

fun <V> HashMap<TypedKey<*>, Any?>.addToListProperty(key: TypedKey<in List<V>>, values: Iterable<V>) {
    val newValues = get(key)?.let { (it as List<V>) + values } ?: values.toList()
    put(key, newValues)
}

fun <V> HashMap<TypedKey<*>, Any?>.addToListProperty(key: TypedKey<in List<V>>, vararg values: V) {
    val newValues = get(key)?.let { (it as List<V>) + values } ?: values.toList()
    put(key, newValues)
}

fun <K, V> HashMap<TypedKey<*>, Any?>.addToMapProperty(key: TypedKey<in Map<K, V>>, values: Map<K, V>) {
    val newValues = get(key)?.let { (it as Map<K, V>) + values } ?: values
    put(key, newValues)
}

fun <K, V> HashMap<TypedKey<*>, Any?>.addToMapProperty(key: TypedKey<in Map<K, V>>, values: Iterable<Pair<K, V>>) {
    val newValues = get(key)?.let { (it as Map<K, V>) + values } ?: values
    put(key, newValues)
}
