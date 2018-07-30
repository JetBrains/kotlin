/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.misc

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.util.PropertyKey


inline operator fun <reified T> PropertyKey<T>.invoke(v: T): Pair<PropertyKey<T>, T> = this to v

inline operator fun <reified K> PropertyKey<KotlinType>.invoke(): Pair<PropertyKey<KotlinType>, KotlinType> =
    this to KotlinType(K::class)

operator fun PropertyKey<KotlinType>.invoke(kclass: KClass<*>): Pair<PropertyKey<KotlinType>, KotlinType> =
    this to KotlinType(kclass)

operator fun PropertyKey<KotlinType>.invoke(ktype: KType): Pair<PropertyKey<KotlinType>, KotlinType> =
    this to KotlinType(ktype)

operator fun PropertyKey<KotlinType>.invoke(fqname: String): Pair<PropertyKey<KotlinType>, KotlinType> =
    this to KotlinType(fqname)

operator fun PropertyKey<List<KotlinType>>.invoke(vararg classes: KClass<*>): Pair<PropertyKey<List<KotlinType>>, List<KotlinType>> =
    this to classes.map { KotlinType(it) }

operator fun PropertyKey<List<KotlinType>>.invoke(vararg types: KType): Pair<PropertyKey<List<KotlinType>>, List<KotlinType>> =
    this to types.map { KotlinType(it) }

operator fun PropertyKey<List<KotlinType>>.invoke(vararg fqnames: String): Pair<PropertyKey<List<KotlinType>>, List<KotlinType>> =
    this to fqnames.map { KotlinType(it) }

inline operator fun <reified E> PropertyKey<List<E>>.invoke(vararg vs: E): Pair<PropertyKey<List<E>>, List<E>> = this to vs.toList()

@JvmName("invoke_kotlintype_map_from_kclass")
inline operator fun <reified K> PropertyKey<Map<K, KotlinType>>.invoke(vararg classes: Pair<K, KClass<*>>): Pair<PropertyKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to LinkedHashMap<K, KotlinType>().also { it.putAll(classes.asSequence().map { (k, v) -> k to KotlinType(v) }) }

@JvmName("invoke_kotlintype_map_from_ktype")
inline operator fun <reified K> PropertyKey<Map<K, KotlinType>>.invoke(vararg types: Pair<K, KType>): Pair<PropertyKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to LinkedHashMap<K, KotlinType>().also { it.putAll(types.asSequence().map { (k, v) -> k to KotlinType(v) }) }

@JvmName("invoke_kotlintype_map_from_fqname")
inline operator fun <reified K> PropertyKey<Map<K, KotlinType>>.invoke(vararg fqnames: Pair<K, String>): Pair<PropertyKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to LinkedHashMap<K, KotlinType>().also { it.putAll(fqnames.asSequence().map { (k, v) -> k to KotlinType(v) }) }

inline operator fun <reified K, reified V> PropertyKey<Map<K, V>>.invoke(vararg vs: Pair<K, V>): Pair<PropertyKey<Map<K, V>>, Map<K, V>> =
    this to mapOf(*vs)

// TODO: make tests from examples below
/*
val x = with(kotlin.script.experimental.api.ScriptingEnvironmentProperties) {
    baseClass<String>()
}

val y = with(kotlin.script.experimental.api.ScriptCompileConfigurationParams) {
    importedPackages("a1", "a2")
}
*/