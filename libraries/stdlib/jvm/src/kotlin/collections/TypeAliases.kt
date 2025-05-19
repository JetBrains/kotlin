/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("ACTUAL_WITHOUT_EXPECT", "ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT") // for building kotlin-stdlib-jvm-minimal-for-test

package kotlin.collections

/**
 * Marker interface indicating that the [List] implementation supports fast indexed access.
 */
@SinceKotlin("1.1") public actual typealias RandomAccess = java.util.RandomAccess


@SinceKotlin("1.1") public actual typealias ArrayList<E> = java.util.ArrayList<E>
@SinceKotlin("1.1") public actual typealias LinkedHashMap<K, V> = java.util.LinkedHashMap<K, V>
@SinceKotlin("1.1") public actual typealias HashMap<K, V> = java.util.HashMap<K, V>
@SinceKotlin("1.1") public actual typealias LinkedHashSet<E> = java.util.LinkedHashSet<E>
@SinceKotlin("1.1") public actual typealias HashSet<E> = java.util.HashSet<E>
