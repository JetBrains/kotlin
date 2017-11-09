/*
 * Copyright 2010-2016 JetBrains s.r.o.
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
package kotlin.collections

/**
 * Groups elements from the [Grouping] source by key and counts elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
public fun <T, K> Grouping<T, K>.eachCount(): Map<K, Int> =
        fold(0) { acc, _ -> acc + 1 }

/**
/**
 * Groups elements from the [Grouping] source by key and sums values provided by the [valueSelector] function for elements in each group.
 *
 * @return a [Map] associating the key of each group with the count of element in the group.
 */
@SinceKotlin("1.1")
public inline fun <T, K> Grouping<T, K>.eachSumOf(valueSelector: (T) -> Int): Map<K, Int> =
        fold(0) { acc, e -> acc + valueSelector(e) }
*/