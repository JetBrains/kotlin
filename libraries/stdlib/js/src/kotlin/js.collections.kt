/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.js.collections


/**
 * Exposes the TypeScript [ReadonlyArray](https://www.typescriptlang.org/docs/handbook/2/objects.html#the-readonlyarray-type) to Kotlin.
 */
@JsName("ReadonlyArray")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external interface JsReadonlyArray<out E>

/**
 * Exposes the JavaScript [Array](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array) to Kotlin.
 */
@JsName("Array")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external open class JsArray<E> : JsReadonlyArray<E>

/**
 * Exposes the TypeScript [ReadonlySet](https://github.com/microsoft/TypeScript/blob/bd952a7a83ce04b3541b952238b6c0e4316b7d5d/src/lib/es2015.collection.d.ts#L103) to Kotlin.
 */
@JsName("ReadonlySet")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external interface JsReadonlySet<out E>

/**
 * Exposes the JavaScript [Set](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Set) to Kotlin.
 */
@JsName("Set")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external open class JsSet<E> : JsReadonlySet<E>

/**
 * Exposes the TypeScript [ReadonlyMap](https://github.com/microsoft/TypeScript/blob/bd952a7a83ce04b3541b952238b6c0e4316b7d5d/src/lib/es2015.collection.d.ts#L37) to Kotlin.
 */
@JsName("ReadonlyMap")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external interface JsReadonlyMap<K, out V>

/**
 * Exposes the JavaScript [Map](https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Map) to Kotlin.
 */
@JsName("Map")
@SinceKotlin("1.9")
@ExperimentalJsCollectionsApi
public external open class JsMap<K, V> : JsReadonlyMap<K, V>