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

package kotlin.aliases

/**
 * This file contains different aliases and shortcuts
 * for functions in the standart Kotlin library.
 * This aliases let users to use a short syntax
 * for some standart functions.
 *
 * All functionality of this library is avaliable
 * in the standart Koltin library via full-named
 * functions.
 *
 * This library is experimental, and the content
 * may be changed without keeping compatibility,
 * so it is not recommended to use this aliases
 * in industrial projects.
 *
 * This library is not supposed to be included in
 * the standart Kotlin library and must be imported
 * manually.
<<<<<<< HEAD
 * */

/*
* Short syntax for .slice() functions
* */
public fun <T> List<T>.get(indexes: IntRange)     : List<T> = slice(indexes)
public fun <T> List<T>.get(indexes: Iterable<Int>): List<T> = slice(indexes)

public fun String.get(indexes: Iterable<Int>)     : String  = slice(indexes)

public fun <K, V> Map<K, V>.get(keys: Iterable<K>): List<V?> = slice(keys)

public fun <T> Array<T>.get(indexes: Iterable<Int>): List<T>        = slice(indexes)
public fun    ByteArray.get(indexes: Iterable<Int>): List<Byte>     = slice(indexes)
public fun   ShortArray.get(indexes: Iterable<Int>): List<Short>    = slice(indexes)
public fun     IntArray.get(indexes: Iterable<Int>): List<Int>      = slice(indexes)
public fun    LongArray.get(indexes: Iterable<Int>): List<Long>     = slice(indexes)
public fun   FloatArray.get(indexes: Iterable<Int>): List<Float>    = slice(indexes)
public fun  DoubleArray.get(indexes: Iterable<Int>): List<Double>   = slice(indexes)
public fun BooleanArray.get(indexes: Iterable<Int>): List<Boolean>  = slice(indexes)





=======
 * */
>>>>>>> 6beff04... created Aliases.kt
