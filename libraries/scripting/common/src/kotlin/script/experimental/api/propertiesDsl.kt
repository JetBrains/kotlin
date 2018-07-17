/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.util.ChainedPropertyBag
import kotlin.script.experimental.util.TypedKey

interface PropertiesGroup 

open class ScriptingProperties(body: ScriptingProperties.() -> Unit = {}) {

    var parentPropertiesBag: ChainedPropertyBag? = null
    var parentPropertiesBuilder: ScriptingProperties? = null
    val data = HashMap<TypedKey<*>, Any?>()

    init {
        body()
    }

    internal fun makePropertyBag(): ChainedPropertyBag =
        ChainedPropertyBag.createOptimized(parentPropertiesBag ?: parentPropertiesBuilder?.makePropertyBag(), data)

    // generic invoke for properties groups
    inline operator fun <T : PropertiesGroup> T.invoke(body: T.() -> Unit) = body()

    // chaining

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

    // inclusion

    fun include(props: ScriptingProperties) {
        data.putAll(props.data)
    }

    fun include(props: ChainedPropertyBag) {
        data.putAll(props.data)
    }

    // builders for known property types:

    inline operator fun <reified T> TypedKey<T>.invoke(v: T) {
        data[this] = v
    }

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

    operator fun TypedKey<List<KotlinType>>.invoke(vararg classes: KClass<*>) {
        data[this] = classes.map { KotlinType(it) }
    }

    operator fun TypedKey<List<KotlinType>>.invoke(vararg types: KType) {
        data[this] = types.map { KotlinType(it) }
    }

    operator fun TypedKey<List<KotlinType>>.invoke(vararg fqnames: String) {
        data[this] = fqnames.map { KotlinType(it) }
    }

    inline operator fun <reified E> TypedKey<List<E>>.invoke(vararg vs: E) {
        data[this] = vs.toList()
    }

    @JvmName("invoke_kotlintype_map_from_kclass")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg classes: Pair<K, KClass<*>>) {
        data[this] = HashMap<K, KotlinType>().also { it.putAll(classes.asSequence().map { (k, v) -> k to KotlinType(v) }) }
    }

    @JvmName("invoke_kotlintype_map_from_ktype")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg types: Pair<K, KType>) {
        data[this] = HashMap<K, KotlinType>().also { it.putAll(types.asSequence().map { (k, v) -> k to KotlinType(v) }) }
    }

    @JvmName("invoke_kotlintype_map_from_fqname")
    inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg fqnames: Pair<K, String>) {
        data[this] = HashMap<K, KotlinType>().also { it.putAll(fqnames.asSequence().map { (k, v) -> k to KotlinType(v) }) }
    }

    inline operator fun <reified K, reified V> TypedKey<Map<K, V>>.invoke(vararg vs: Pair<K, V>) {
        data[this] = hashMapOf(*vs)
    }
}
