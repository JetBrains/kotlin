/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.js.inline.util

import java.util.Collections
import java.util.IdentityHashMap

fun <T> IdentitySet(): MutableSet<T> {
    return Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
}

fun <T> Collection<T>.toIdentitySet(): MutableSet<T> {
    val result = IdentitySet<T>()
    for (element in this) {
        result.add(element)
    }

    return result
}

fun <T> Sequence<T>.toIdentitySet(): MutableSet<T> {
    val result = IdentitySet<T>()
    for (element in this) {
        result.add(element)
    }

    return result
}

fun <T, R> Iterable<T>.zipWithDefault(
        other: Iterable<R>,
        defaultT: T
): List<Pair<T, R>> {

    val itT = iterator()
    val itR = other.iterator()

    val result = arrayListOf<Pair<T, R>>()

    while (itT.hasNext() && itR.hasNext()) {
        result.add(itT.next() to itR.next())
    }

    assert(!itT.hasNext()) { "First collection is bigger than second" }

    while (itR.hasNext()) {
        result.add(defaultT to itR.next())
    }

    return result
}
