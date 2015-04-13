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

package kotlin

/**
 * Concatenates this Char and a String.
 */
public fun Char.plus(string: String) : String = this.toString() + string

/**
 * Returns `true` if this character is equal to the [other] character, optionally ignoring character case.
 *
 * @param ignoreCase `true` to ignore character case when comparing characters. By default `false`.
 *
 * Two characters are considered the same ignoring case if at least one of the following is true:
 *   - The two characters are the same (as compared by the == operator)
 *   - Applying the method [toUpperCase] to each character produces the same result
 *   - Applying the method [toLowerCase] to each character produces the same result
 */
public fun Char.equals(other: Char, ignoreCase: Boolean = false): Boolean {
    if (this === other) return true
    if (!ignoreCase) return false

    if (this.toUpperCase() === other.toUpperCase()) return true
    if (this.toLowerCase() === other.toLowerCase()) return true
    return false
}

/**
 * Returns `true` if this character is a Unicode high-surrogate code unit (also known as leading-surrogate code unit).
 */
public fun Char.isHighSurrogate(): Boolean = this in Char.MIN_HIGH_SURROGATE..Char.MAX_HIGH_SURROGATE

/**
 * Returns `true` if this character is a Unicode low-surrogate code unit (also known as trailing-surrogate code unit).
 */
public fun Char.isLowSurrogate(): Boolean = this in Char.MIN_LOW_SURROGATE..Char.MAX_LOW_SURROGATE

/**
 * Returns `true` if this character is a Unicode surrogate code unit.
 */
public fun Char.isSurrogate(): Boolean = this in Char.MIN_SURROGATE..Char.MAX_SURROGATE

/**
 * The minimum value of a Unicode high-surrogate code unit.
 */
public val Char.Companion.MIN_HIGH_SURROGATE: Char
    get() = '\uD800'

/**
 * The maximum value of a Unicode high-surrogate code unit.
 */
public val Char.Companion.MAX_HIGH_SURROGATE: Char
    get() = '\uDBFF'

/**
 * The minimum value of a Unicode low-surrogate code unit.
 */
public val Char.Companion.MIN_LOW_SURROGATE: Char
    get() = '\uDC00'

/**
 * The maximum value of a Unicode low-surrogate code unit.
 */
public val Char.Companion.MAX_LOW_SURROGATE: Char
    get() = '\uDFFF'

/**
 * The minimum value of a Unicode surrogate code unit.
 */
public val Char.Companion.MIN_SURROGATE: Char
    get() = MIN_HIGH_SURROGATE

/**
 * The maximum value of a Unicode surrogate code unit.
 */
public val Char.Companion.MAX_SURROGATE: Char
    get() = MAX_LOW_SURROGATE
