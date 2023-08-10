/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.collections

@JsName("Array")
internal external abstract class JsMutableArray<E> : JsImmutableArray<E>

@JsName("ReadonlyArray")
internal external interface JsImmutableArray<out E>

@JsName("ReadonlySet")
internal external interface JsImmutableSet<out E>

@JsName("Set")
internal external abstract class JsMutableSet<E> : JsImmutableSet<E>

@JsName("ReadonlyMap")
internal external interface JsImmutableMap<K, out V>

@JsName("Map")
internal external abstract class JsMutableMap<K, V> : JsImmutableMap<K, V>