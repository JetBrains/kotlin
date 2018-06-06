/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.misc

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.script.experimental.api.KotlinType
import kotlin.script.experimental.util.TypedKey


inline operator fun <reified T> TypedKey<T>.invoke(v: T): Pair<TypedKey<T>, T> = this to v

inline operator fun <reified K> TypedKey<KotlinType>.invoke(): Pair<TypedKey<KotlinType>, KotlinType> =
    this to KotlinType(K::class)

operator fun TypedKey<KotlinType>.invoke(kclass: KClass<*>): Pair<TypedKey<KotlinType>, KotlinType> =
    this to KotlinType(kclass)

operator fun TypedKey<KotlinType>.invoke(ktype: KType): Pair<TypedKey<KotlinType>, KotlinType> =
    this to KotlinType(ktype)

operator fun TypedKey<KotlinType>.invoke(fqname: String): Pair<TypedKey<KotlinType>, KotlinType> =
    this to KotlinType(fqname)

operator fun TypedKey<List<KotlinType>>.invoke(vararg classes: KClass<*>): Pair<TypedKey<List<KotlinType>>, List<KotlinType>> =
    this to classes.map { KotlinType(it) }

operator fun TypedKey<List<KotlinType>>.invoke(vararg types: KType): Pair<TypedKey<List<KotlinType>>, List<KotlinType>> =
    this to types.map { KotlinType(it) }

operator fun TypedKey<List<KotlinType>>.invoke(vararg fqnames: String): Pair<TypedKey<List<KotlinType>>, List<KotlinType>> =
    this to fqnames.map { KotlinType(it) }

inline operator fun <reified E> TypedKey<List<E>>.invoke(vararg vs: E): Pair<TypedKey<List<E>>, List<E>> = this to vs.toList()

@JvmName("invoke_kotlintype_map_from_kclass")
inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg classes: Pair<K, KClass<*>>): Pair<TypedKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to HashMap<K, KotlinType>().also { it.putAll(classes.asSequence().map { (k, v) -> k to KotlinType(v) }) }

@JvmName("invoke_kotlintype_map_from_ktype")
inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg types: Pair<K, KType>): Pair<TypedKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to HashMap<K, KotlinType>().also { it.putAll(types.asSequence().map { (k, v) -> k to KotlinType(v) }) }

@JvmName("invoke_kotlintype_map_from_fqname")
inline operator fun <reified K> TypedKey<Map<K, KotlinType>>.invoke(vararg fqnames: Pair<K, String>): Pair<TypedKey<Map<K, KotlinType>>, Map<K, KotlinType>> =
    this to HashMap<K, KotlinType>().also { it.putAll(fqnames.asSequence().map { (k, v) -> k to KotlinType(v) }) }

inline operator fun <reified K, reified V> TypedKey<Map<K, V>>.invoke(vararg vs: Pair<K, V>): Pair<TypedKey<Map<K, V>>, Map<K, V>> =
    this to hashMapOf(*vs)

// TODO: make tests from examples below
/*
val x = with(kotlin.script.experimental.api.ScriptingEnvironmentProperties) {
    baseClass<String>()
}

val y = with(kotlin.script.experimental.api.ScriptCompileConfigurationParams) {
    importedPackages("a1", "a2")
}
*/