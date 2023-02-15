/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("EXPECT_AND_ACTUAL_IN_THE_SAME_MODULE")

package test.collections.js

// TODO: These have actuals only in JVM tests, and in JS they are in stdlib
public expect fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V>
public expect fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V>
public expect fun stringSetOf(vararg elements: String): HashSet<String>
public expect fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String>
