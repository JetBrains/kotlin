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

fun specialJVM(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("fill(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Fills original array with the provided value." }
        returns { "Unit" }
        body {
            """
            java.util.Arrays.fill(this, fromIndex, toIndex, element)
            """
        }
    }

    templates add f("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc {
            """
            Searches the array or the range of the array for the provided [element] using the binary search algorithm.
            The array is expected to be sorted, otherwise the result is undefined.

            If the array contains multiple elements equal to the specified [element], there is no guarantee which one will be found.

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted.
            """
        }
        returns("Int")
        body {
            "return java.util.Arrays.binarySearch(this, fromIndex, toIndex, element)"
        }
    }

    templates add f("binarySearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects)
        doc {
            """
            Searches the array or the range of the array for the provided [element] using the binary search algorithm.
            The array is expected to be sorted according to the specified [comparator], otherwise the result is undefined.

            If the array contains multiple elements equal to the specified [element], there is no guarantee which one will be found.

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted according to the specified [comparator].
            """
        }
        returns("Int")
        body {
            "return java.util.Arrays.binarySearch(this, fromIndex, toIndex, element, comparator)"
        }
    }


    templates add f("sort()") {
        // left with more generic signature for JVM only
        only(ArraysOfObjects)
        doc {
            """
            Sorts the array in-place according to the natural order of its elements.

            @throws ClassCastException if any element of the array is not [Comparable].
            """
        }
        returns("Unit")
        body {
            "if (size > 1) java.util.Arrays.sort(this)"
        }
    }

    templates add f("sort(fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts a range in the array in-place." }
        returns("Unit")
        body {
            "java.util.Arrays.sort(this, fromIndex, toIndex)"
        }
    }

    templates add f("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        only(ArraysOfObjects)
        doc { "Sorts a range in the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "java.util.Arrays.sort(this, fromIndex, toIndex, comparator)"
        }
    }

    templates add f("filterIsInstanceTo(destination: C, klass: Class<R>)") {
        doc { "Appends all elements that are instances of specified class to the given [destination]." }
        receiverAsterisk(true)
        typeParam("C : MutableCollection<in R>")
        typeParam("R")
        returns("C")
        exclude(ArraysOfPrimitives, Strings)
        body {
            """
            @Suppress("UNCHECKED_CAST")
            for (element in this) if (klass.isInstance(element)) destination.add(element as R)
            return destination
            """
        }
    }

    templates add f("filterIsInstance(klass: Class<R>)") {
        doc { "Returns a list containing all elements that are instances of specified class." }
        receiverAsterisk(true)
        typeParam("R")
        returns("List<R>")
        body {
            """
            return filterIsInstanceTo(ArrayList<R>(), klass)
            """
        }
        exclude(ArraysOfPrimitives, Strings)

        doc(Sequences) { "Returns a sequence containing all elements that are instances of specified class." }
        returns(Sequences) { "Sequence<R>" }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filter { klass.isInstance(it) } as Sequence<R>
            """
        }
    }

    templates.forEach { it.apply { jvmOnly(true) } }

    return templates
}

object CommonArrays {

    fun f_plusElement() = f("plusElement(element: T)") {
        inline(Platform.JVM, Inline.Only)
        inline(Platform.JS, Inline.Yes)
        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

        only(InvariantArraysOfObjects)
        only(Platform.JS, ArraysOfObjects)

        returns("SELF")
        returns(Platform.JS, ArraysOfObjects) { "Array<T>" }
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body(Platform.JVM) { "return plus(element)" }
        body(Platform.JS) {
            """
            return this.asDynamic().concat(arrayOf(element))
            """
        }
    }

    fun f_plusElementOperator() =
            (listOf(InvariantArraysOfObjects to null) + PrimitiveType.defaultPrimitives.map { ArraysOfPrimitives to it }).map {
                val (family, primitive) = it
                f("plus(element: T)") {
                    operator(true)

                    only(family)
                    if (family == InvariantArraysOfObjects) {
                        only(Platform.JS, ArraysOfObjects)
                    }

                    inline(Platform.JS, Inline.Yes)
                    annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

                    returns("SELF")
                    returns(Platform.JS, ArraysOfObjects) { "Array<T>" }
                    doc { "Returns an array containing all elements of the original array and then the given [element]." }
                    body(Platform.JVM) {
                        """
                        val index = size
                        val result = java.util.Arrays.copyOf(this, index + 1)
                        result[index] = element
                        return result
                        """
                    }

                    if (primitive == null) {
                        body(Platform.JS) { "return this.asDynamic().concat(arrayOf(element))" }
                    }
                    else {
                        only(primitive)
                        body(Platform.JS, ArraysOfPrimitives) { "return plus(${primitive.name.toLowerCase()}ArrayOf(element))" }
                    }
                }
            }

    fun f_plusCollection() = f("plus(elements: Collection<T>)") {
        operator(true)

        // TODO: inline arrayPlusCollection when @PublishedAPI is available
//        inline(Platform.JS, Inline.Yes)
//        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        only(Platform.JS, ArraysOfObjects, ArraysOfPrimitives)

        returns("SELF")
        returns(Platform.JS, ArraysOfObjects) { "Array<T>" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        body(Platform.JVM) {
            """
            var index = size
            val result = java.util.Arrays.copyOf(this, index + elements.size)
            for (element in elements) result[index++] = element
            return result
            """
        }
        body(Platform.JS) {
            """
            return arrayPlusCollection(this, elements)
            """
        }
    }

    fun f_plusArray() = f("plus(elements: SELF)") {
        operator(true)

        inline(Platform.JS, Inline.Yes)
        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        only(Platform.JS, ArraysOfObjects, ArraysOfPrimitives)
        customSignature(InvariantArraysOfObjects) { "plus(elements: Array<out T>)" }

        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        returns("SELF")
        returns(Platform.JS, ArraysOfObjects) { "Array<T>" }

        body(Platform.JVM) {
            """
            val thisSize = size
            val arraySize = elements.size
            val result = java.util.Arrays.copyOf(this, thisSize + arraySize)
            System.arraycopy(elements, 0, result, thisSize, arraySize)
            return result
            """
        }
        body(Platform.JS) {
            """
            return this.asDynamic().concat(elements)
            """
        }
        body(Platform.JS, ArraysOfPrimitives) {
            """
            return primitiveArrayConcat(this, elements)
            """
        }

    }

    fun f_copyOfRange() = f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        inline(Platform.JVM, Inline.Only)
        inline(Platform.JS, Inline.Yes)
        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        only(Platform.JS, ArraysOfObjects, ArraysOfPrimitives)

        doc { "Returns new array which is a copy of range of original array." }
        returns("SELF")
        returns(Platform.JS, ArraysOfObjects) { "Array<T>" }
        body(Platform.JVM) {
            "return java.util.Arrays.copyOfRange(this, fromIndex, toIndex)"
        }
        body(Platform.JS) {
            // TODO: Arguments checking as in java?
            "return this.asDynamic().slice(fromIndex, toIndex)"
        }
    }

    fun f_copyOf() = f("copyOf()") {
        inline(Platform.JVM, Inline.Only)
        inline(Platform.JS, Inline.Yes)
        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        only(Platform.JS, ArraysOfObjects, ArraysOfPrimitives)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        returns(Platform.JS, ArraysOfObjects) { "Array<T>" }

        body(Platform.JVM) {
            "return java.util.Arrays.copyOf(this, size)"
        }
        body(Platform.JS) {
            "return this.asDynamic().slice()"
        }
    }

    fun f_copyOfResized() =
        (PrimitiveType.defaultPrimitives.map { ArraysOfPrimitives to it } + (InvariantArraysOfObjects to null)).map {
            val (family, primitive) = it
            f("copyOf(newSize: Int)") {
                only(family)
                if (family == InvariantArraysOfObjects) {
                    only(Platform.JS, ArraysOfObjects)
                }

                inline(Platform.JVM, Inline.Only)
                doc { "Returns new array which is a copy of the original array, resized to the given [newSize]." }
                val defaultValue: String
                if (primitive != null) {
                    only(primitive)
                    returns("SELF")
                    defaultValue = when (primitive) {
                        PrimitiveType.Boolean -> false.toString()
                        PrimitiveType.Char -> "0"
                        else -> "ZERO"
                    }
                } else {
                    returns { "Array<T?>" }
                    defaultValue = "null"
                }
                body(Platform.JVM) {
                    "return java.util.Arrays.copyOf(this, newSize)"
                }
                body(Platform.JS) {
                    """
                    return arrayCopyResize(this, newSize, $defaultValue)
                    """
                }
            }
        }

    fun f_sortPrimitives() =
        (PrimitiveType.numericPrimitives + PrimitiveType.Char).map { primitive ->
            f("sort()") {
                only(ArraysOfPrimitives)
                only(primitive)
                doc { "Sorts the array in-place." }
                returns("Unit")
                body(Platform.JVM) {
                    "if (size > 1) java.util.Arrays.sort(this)"
                }
                if (primitive != PrimitiveType.Long) {
                    annotations(Platform.JS, """@library("primitiveArraySort")""")
                    body(Platform.JS) { "definedExternally" }
                }
                else {
                    body(Platform.JS) {
                        """
                        if (size > 1)
                            sort { a: T, b: T -> a.compareTo(b) }
                        """
                    }
                }
            }
        }

    fun f_sort() = f("sort()") {
        only(ArraysOfObjects)
        typeParam("T: Comparable<T>")
        doc {
            """
            Sorts the array in-place according to the natural order of its elements.
            """
        }
        returns("Unit")
        inline(Platform.JVM, Inline.Only)
        body(Platform.JVM) {
            "(this as Array<Any?>).sort()"
        }
        body(Platform.JS) {
            """
            if (size > 1)
                sort { a: T, b: T -> a.compareTo(b) }
            """
        }
    }

    fun f_sortWith() = f("sortWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjects)
        buildFamilyPrimitives(Platform.JS, buildFamilyPrimitives.default!! - PrimitiveType.Boolean)
        doc { "Sorts the array in-place according to the order specified by the given [comparator]." }
        returns("Unit")
        body(Platform.JVM) {
            "if (size > 1) java.util.Arrays.sort(this, comparator)"
        }
        body(Platform.JS) {
            """
            if (size > 1)
                sort { a, b -> comparator.compare(a, b) }
            """
        }
    }

    fun f_asList() = f("asList()") {
        only(ArraysOfObjects)
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        body(Platform.JVM) {
            """
            return ArraysUtilJVM.asList(this)
            """
        }

        body(Platform.JS) {
            """
            return ArrayList<T>(this.unsafeCast<Array<Any?>>())
            """
        }
    }

    fun f_asListPrimitives() =
            PrimitiveType.defaultPrimitives.map { primitive ->
                f("asList()") {
                    only(ArraysOfPrimitives)
                    only(primitive)
                    doc { "Returns a [List] that wraps the original array." }
                    returns("List<T>")

                    val objectLiteralImpl = """
                        return object : AbstractList<T>(), RandomAccess {
                            override val size: Int get() = this@asList.size
                            override fun isEmpty(): Boolean = this@asList.isEmpty()
                            override fun contains(element: T): Boolean = this@asList.contains(element)
                            override fun get(index: Int): T = this@asList[index]
                            override fun indexOf(element: T): Int = this@asList.indexOf(element)
                            override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
                        }
                        """

                    body(Platform.JVM) { objectLiteralImpl }

                    if (primitive == PrimitiveType.Char) {
                        body(Platform.JS) { objectLiteralImpl }
                    }
                    else {
                        inline(Platform.JS, Inline.Yes, ArraysOfPrimitives)
                        body(Platform.JS) { "return this.unsafeCast<Array<T>>().asList()" }
                    }
                }
            }

    fun f_toTypedArray() =
            PrimitiveType.defaultPrimitives.map { primitive ->
                f("toTypedArray()") {
                    only(ArraysOfPrimitives)
                    only(primitive)
                    returns("Array<T>")
                    doc {
                        """
                        Returns a *typed* object array containing all of the elements of this primitive array.
                        """
                    }
                    body(Platform.JVM) {
                        """
                        val result = arrayOfNulls<T>(size)
                        for (index in indices)
                            result[index] = this[index]
                        @Suppress("UNCHECKED_CAST")
                        return result as Array<T>
                        """
                    }

                    if (primitive == PrimitiveType.Char) {
                        body(Platform.JS) { "return Array<Char>(size, { i -> this[i] })" }
                    }
                    else {
                        body(Platform.JS) { "return copyOf().unsafeCast<Array<T>>()" }
                    }
                }
            }


    // TODO: use reflection later to get all functions of matching type
    fun templates() =
            listOf(f_plusElement()) +
            f_plusElementOperator() +
            listOf(f_plusCollection(), f_plusArray(), f_copyOf(), f_copyOfRange()) +
            f_copyOfResized() +
            f_sortPrimitives() +
            listOf(f_sort(), f_sortWith(), f_asList()) +
            f_asListPrimitives() +
            f_toTypedArray()
}