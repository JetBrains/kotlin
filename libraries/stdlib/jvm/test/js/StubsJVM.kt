/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test.collections.js

public actual fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> = hashMapOf<String, V>(*pairs)
public actual fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> = linkedMapOf(*pairs)
public actual fun stringSetOf(vararg elements: String): HashSet<String> = hashSetOf(*elements)
public actual fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> = linkedSetOf(*elements)
