/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kt54119

class KT54119KotlinKey

private typealias Foo = KT54119KotlinKey

fun callContains(set: Set<*>) = set.contains(Foo())

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
fun callGetElement(set: Set<*>) = (set as kotlin.native.internal.KonanSet<Any?>).getElement(Foo())

fun callContainsKey(map: Map<*, *>) = map.containsKey(Foo())

fun callContainsValue(map: Map<*, *>) = map.containsValue(Foo())

fun callGet(map: Map<*, *>) = map.get(Foo())

fun callGetOrThrowConcurrentModification(map: Map<*, *>) = map.hashCode() // calls getOrThrowConcurrentModification under the hood.
fun callContainsEntry(map: Map<*, *>) = map.entries.contains(map.entries.first())
