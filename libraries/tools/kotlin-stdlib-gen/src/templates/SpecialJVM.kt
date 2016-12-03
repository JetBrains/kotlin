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

    templates add f("plusElement(element: T)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body { "return plus(element)" }
    }

    templates add f("plus(element: T)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            val index = size
            val result = java.util.Arrays.copyOf(this, index + 1)
            result[index] = element
            return result
            """
        }
    }

    templates add f("plus(elements: Collection<T>)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        body {
            """
            var index = size
            val result = java.util.Arrays.copyOf(this, index + elements.size)
            for (element in elements) result[index++] = element
            return result
            """
        }
    }

    templates add f("plus(elements: SELF)") {
        operator(true)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        customSignature(InvariantArraysOfObjects) { "plus(elements: Array<out T>)" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        returns("SELF")
        body {
            """
            val thisSize = size
            val arraySize = elements.size
            val result = java.util.Arrays.copyOf(this, thisSize + arraySize)
            System.arraycopy(elements, 0, result, thisSize, arraySize)
            return result
            """
        }
    }


    templates add f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        returns("SELF")
        body {
            "return java.util.Arrays.copyOfRange(this, fromIndex, toIndex)"
        }
    }

    templates add f("copyOf()") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        body {
            "return java.util.Arrays.copyOf(this, size)"
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("copyOf(newSize: Int)") {
        inline(Inline.Only)

        only(InvariantArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        returns(InvariantArraysOfObjects) { "Array<T?>" }
        body {
            "return java.util.Arrays.copyOf(this, newSize)"
        }
    }

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
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts the array in-place." }
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

    templates add f("sortWith(comparator: Comparator<in T>)") {
        only(ArraysOfObjects)
        doc { "Sorts the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "if (size > 1) java.util.Arrays.sort(this, comparator)"
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

    templates add f("asList()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        body(ArraysOfObjects) {
            """
            return ArraysUtilJVM.asList(this)
            """
        }

        body(ArraysOfPrimitives) {
            """
            return object : AbstractList<T>(), RandomAccess {
                override val size: Int get() = this@asList.size
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(element: T): Boolean = this@asList.contains(element)
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(element: T): Int = this@asList.indexOf(element)
                override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
            }
            """
        }
//
//        body(ArraysOfObjects) {
//            """
//            return ArrayList<T>(this.unsafeCast<Array<Any?>>())
//            """
//        }
//
//        inline(true, ArraysOfPrimitives)
//        body(ArraysOfPrimitives) {"""return this.unsafeCast<Array<T>>().asList()"""}

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
            val result = arrayOfNulls<T>(size)
            for (index in indices)
                result[index] = this[index]
            @Suppress("UNCHECKED_CAST")
            return result as Array<T>
            """
        }
//        body {
//            """
//            return copyOf().unsafeCast<Array<T>>()
//            """
//        }
    }

    templates.forEach { it.apply { jvmOnly(true) } }

    return templates
}