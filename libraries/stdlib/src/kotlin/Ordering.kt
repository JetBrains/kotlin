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
 * Helper method for implementing [[Comparable]] methods using a list of functions
 * to calculate the values to compare
 */
public fun <T : Any> compareBy(a: T?, b: T?, vararg functions: T.() -> Comparable<*>?): Int {
    require(functions.size > 0)
    if (a identityEquals b) return 0
    if (a == null) return - 1
    if (b == null) return 1
    for (fn in functions) {
        val v1 = a.fn()
        val v2 = b.fn()
        val diff = compareValues(v1, v2)
        if (diff != 0) return diff
    }
    return 0
}

/**
 * Compares the two values which may be [[Comparable]] otherwise
 * they are compared via [[#equals()]] and if they are not the same then
 * the [[#hashCode()]] method is used as the difference
 */
public fun <T : Comparable<*>> compareValues(a: T?, b: T?): Int {
    if (a identityEquals b) return 0
    if (a == null) return - 1
    if (b == null) return 1

    return (a as Comparable<Any?>).compareTo(b)
}
