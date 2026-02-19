/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package test.collections.js

actual fun <V> stringMapOf(vararg pairs: Pair<String, V>): HashMap<String, V> = hashMapOf<String, V>(*pairs)
actual fun <V> linkedStringMapOf(vararg pairs: Pair<String, V>): LinkedHashMap<String, V> = linkedMapOf(*pairs)
actual fun stringSetOf(vararg elements: String): HashSet<String> = hashSetOf(*elements)
actual fun linkedStringSetOf(vararg elements: String): LinkedHashSet<String> = linkedSetOf(*elements)