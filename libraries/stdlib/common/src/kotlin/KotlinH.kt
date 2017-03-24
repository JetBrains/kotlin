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

package kotlin

import kotlin.annotation.AnnotationTarget.*

open header class Error : Throwable {
    constructor()
    constructor(message: String)
}

open header class Exception : Throwable {
    constructor()
    constructor(message: String)
}

open header class IllegalArgumentException : RuntimeException {
    constructor()
    constructor(message: String)
}

open header class IllegalStateException : RuntimeException {
    constructor()
    constructor(message: String)
}

open header class IndexOutOfBoundsException : RuntimeException {
    constructor()
    constructor(message: String)
}

open header class NoSuchElementException : RuntimeException {
    constructor()
    constructor(message: String)
}

open header class RuntimeException : Exception {
    constructor()
    constructor(message: String)
}

open header class UnsupportedOperationException : RuntimeException {
    constructor()
    constructor(message: String)
}

// TODO: Provide typealias impl in stdlib-jvm
open header class AssertionError : Error {
    constructor()
    constructor(message: String)
}


header interface Comparator<T> {
    fun compare(a: T, b: T): Int
}

header inline fun <T> Comparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T>

// From kotlin.kt

internal header fun <T> arrayOfNulls(reference: Array<out T>, size: Int): Array<T>
internal inline header fun <K, V> Map<K, V>.toSingletonMapOrSelf(): Map<K, V>
internal inline header fun <K, V> Map<out K, V>.toSingletonMap(): Map<K, V>
internal inline header fun <T> Array<out T>.copyToArrayOfAny(isVarargs: Boolean): Array<out Any?>

internal header interface Serializable

// From numbers.kt

header fun Double.isNaN(): Boolean
header fun Float.isNaN(): Boolean
header fun Double.isInfinite(): Boolean
header fun Float.isInfinite(): Boolean
header fun Double.isFinite(): Boolean
header fun Float.isFinite(): Boolean


// From concurrent.kt

@Target(PROPERTY, FIELD)
header annotation class Volatile

inline header fun <R> synchronized(lock: Any, crossinline block: () -> R): R




// from lazy.kt

public header fun <T> lazy(initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [mode] parameter is ignored. */
public header fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T>

/**
 * Creates a new instance of the [Lazy] that uses the specified initialization function [initializer].
 *
 * The [lock] parameter is ignored.
 */
public header fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T>
