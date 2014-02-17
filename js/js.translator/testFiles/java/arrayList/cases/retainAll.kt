/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package foo

import java.util.ArrayList;


// TODO: drop when listOf will be available here.
fun listOf<T>(vararg a: T): List<T> {
    val list = ArrayList<T>();

    for (e in a) {
        list.add(e)
    }

    return list
}

fun test<T>(a: List<T>, b: List<T>, removed: Boolean, expected: List<T>): String? {
    val t = ArrayList<T>(a.size())
    t.addAll(a)

    if (t.retainAll(b) != removed) return "$a.retainAll($b) != $removed, result list: $t"
    if (t != expected) return "Wrong result of $a.retainAll($b), expected: $expected, actual: $t"

    return null
}

fun box(): String {
    val list = listOf(3, "2", -1, null, 0, 8, 5, "3", 77, -15)
    val subset = listOf(3, "2", -1, null)
    val empty = listOf<Any?>()
    val withOtherElements = listOf(3, 54, null)

    return test(list, subset, removed = true, expected = subset) ?:
           test(list, empty, removed = true, expected = empty) ?:
           test(list, withOtherElements, removed = true, expected = listOf(3, null)) ?:
           test(list, list, removed = false, expected = list) ?:
           "OK"
}
