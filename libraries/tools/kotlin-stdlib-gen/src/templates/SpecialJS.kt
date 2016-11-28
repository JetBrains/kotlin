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

package templates

import templates.Family.*

fun specialJS(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("asList()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        body(ArraysOfObjects) {
            """
            return ArrayList<T>(this.unsafeCast<Array<Any?>>())
            """
        }

        inline(true, ArraysOfPrimitives)
        body(ArraysOfPrimitives) {"""return this.unsafeCast<Array<T>>().asList()"""}
    }


    templates add f("toTypedArray()") {
        only(ArraysOfPrimitives)
        returns("Array<T>")
        doc {
            """
            Returns a *typed* object array containing all of the elements of this primitive array.
            """
        }
        body {
            """
            return copyOf().unsafeCast<Array<T>>()
            """
        }
    }

    templates add f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        // TODO: Arguments checking as in java?
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            "return this.asDynamic().slice(fromIndex, toIndex)"
        }
    }

    templates add f("copyOf()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            "return this.asDynamic().slice()"
        }
    }

    val allArrays = PrimitiveType.defaultPrimitives.map { ArraysOfPrimitives to it } + (ArraysOfObjects to null)
    templates addAll allArrays.map {
            val (family, primitive) = it
            f("copyOf(newSize: Int)") {
                only(family)
                val defaultValue: String
                if (primitive != null) {
                    returns("SELF")
                    only(primitive)
                    defaultValue = when (primitive) {
                        PrimitiveType.Boolean -> false.toString()
                        PrimitiveType.Char -> "'\\u0000'"
                        else -> "ZERO"
                    }
                } else {
                    returns(ArraysOfObjects) { "Array<T?>" }
                    defaultValue = "null"
                }
                doc { "Returns new array which is a copy of the original array." }
                body {
                    """
                    return arrayCopyResize(this, newSize, $defaultValue)
                    """
                }
            }
        }


    templates add f("plusElement(element: T)") {
        only(ArraysOfObjects)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            return this.asDynamic().concat(arrayOf(element))
            """
        }
    }

    templates add f("plus(element: T)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            return this.asDynamic().concat(arrayOf(element))
            """
        }
    }

    templates add f("plus(elements: Collection<T>)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        body {
            """
            return arrayPlusCollection(this, elements)
            """
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("plus(elements: SELF)") {
        operator(true)

        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        inline(true)
        annotations("""@Suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            """
            return this.asDynamic().concat(elements)
            """
        }
    }

    templates add f("contentEquals(other: SELF)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *structurally* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            """
        }
        annotations("""@library("arrayEquals")""")
        returns("Boolean")
        body { "return noImpl" }
    }

    templates add f("contentDeepEquals(other: SELF)") {
        only(ArraysOfObjects)
        since("1.1")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *deeply* equal to one another,
            i.e. contain the same number of the same elements in the same order.

            If two corresponding elements are nested arrays, they are also compared deeply.
            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        annotations("""@library("arrayDeepEquals")""")
        returns("Boolean")
        body { "return noImpl" }
    }

    templates add f("contentToString()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        doc { "Returns a string representation of the contents of the specified array as if it is a [List]." }
        annotations("""@library("arrayToString")""")
        returns("String")
        body { "return noImpl" }
    }

    templates add f("contentDeepToString()") {
        only(ArraysOfObjects)
        since("1.1")
        doc {
            """
            Returns a string representation of the contents of this array as if it is a [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level that reference
            is rendered as `"[...]"` to prevent recursion.
            """
        }
        annotations("""@library("arrayDeepToString")""")
        returns("String")
        body { "return noImpl" }
    }

    templates add f("contentHashCode()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        annotations("""@library("arrayHashCode")""")
        returns("Int")
        body { "return noImpl" }
    }

    templates add f("contentDeepHashCode()") {
        only(ArraysOfObjects)
        since("1.1")
        doc {
            """
            Returns a hash code based on the contents of this array as if it is [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        annotations("""@library("arrayDeepHashCode")""")
        returns("Int")
        body { "return noImpl" }
    }


    templates add f("sort(noinline comparison: (T, T) -> Int)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        inline(true)
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparison] function." }
        body { "asDynamic().sort(comparison)" }
    }

    templates add f("sortWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjects)
        exclude(PrimitiveType.Boolean)
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparator] object." }
        body {
            """
            if (size > 1)
                sort { a, b -> comparator.compare(a, b) }
            """
        }
    }

    templates add f("sort()") {
        only(ArraysOfPrimitives)
        only(numericPrimitives + PrimitiveType.Char)
        exclude(PrimitiveType.Long)
        returns("Unit")
        doc { "Sorts the array in-place." }
        annotations("""@library("primitiveArraySort")""")
        body { "noImpl" }
    }

    templates add f("sort()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        only(PrimitiveType.Long)
        typeParam("T: Comparable<T>")
        returns("Unit")
        doc { "Sorts the array in-place." }
        body {
            """
            if (size > 1)
                sort { a: T, b: T -> a.compareTo(b) }
            """
        }
    }


    return templates
}