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

// a package is omitted to get declarations directly under the module

external private fun <T> Array(size: Int): Array<T>

@JsName("newArray")
fun <T> newArray(size: Int, initValue: T): Array<T> {
    return fillArray(Array(size), initValue)
}

private fun <T> fillArray(array: Array<T>, value: T): Array<T> {
    for (i in 0..array.size - 1) {
        array[i] = value
    }
    return array;
}

@JsName("newArrayF")
fun <T> arrayWithFun(size: Int, init: (Int) -> T): Array<T> {
    var result = Array<T>(size)
    for (i in 0..size - 1) {
        result[i] = init(i)
    }
    return result
}

@JsName("newBooleanArray")
fun <T> newBooleanArray(size: Int): BooleanArray = withType(newArray(size, false), "BooleanArray")

@JsName("newBooleanArrayOf")
fun <T> newBooleanArrayOf(vararg bools: Boolean): BooleanArray = withType(bools, "BooleanArray")

@JsName("newCharArray")
fun <T> newCharArray(size: Int): CharArray = withType(newArray(size, 0), "CharArray")

@JsName("newCharArrayOf")
fun <T> newCharArrayOf(vararg chars: Char): CharArray = withType(chars, "CharArray")

@JsName("newLongArray")
fun <T> newLongArray(size: Int): LongArray = withType(newArray(size, js("Kotlin.Long.ZERO")), "LongArray")

@JsName("newLongArrayOf")
fun <T> newLongArrayOf(vararg longs: Long): LongArray = withType(longs, "LongArray")

private fun withType(array: dynamic, type: String): dynamic {
    array.`$type$` = type
    return array
}