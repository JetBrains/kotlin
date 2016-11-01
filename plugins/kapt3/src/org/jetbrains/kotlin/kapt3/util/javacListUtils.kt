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

package org.jetbrains.kotlin.kapt3

import com.sun.tools.javac.util.List as JavacList

internal inline fun <T, R> mapJList(values: Iterable<T>?, f: (T) -> R?): JavacList<R> {
    if (values == null) return JavacList.nil()

    var result = JavacList.nil<R>()
    for (item in values) {
        f(item)?.let { result = result.append(it) }
    }
    return result
}

internal inline fun <T, R> mapJListIndexed(values: Iterable<T>?, f: (Int, T) -> R?): JavacList<R> {
    if (values == null) return JavacList.nil()

    var result = JavacList.nil<R>()
    values.forEachIndexed { index, item ->
        f(index, item)?.let { result = result.append(it) }
    }
    return result
}

internal inline fun <T> mapPairedValuesJList(valuePairs: List<Any>?, f: (String, Any) -> T?): JavacList<T> {
    if (valuePairs == null || valuePairs.isEmpty()) return JavacList.nil()

    val size = valuePairs.size
    var result = JavacList.nil<T>()
    assert(size % 2 == 0)
    var index = 0
    while (index < size) {
        val key = valuePairs[index] as String
        val value = valuePairs[index + 1]
        f(key, value)?.let { result = result.prepend(it) }
        index += 2
    }
    return result.reverse()
}

internal operator fun <T : Any> JavacList<T>.plus(other: JavacList<T>): JavacList<T> {
    return this.appendList(other)
}