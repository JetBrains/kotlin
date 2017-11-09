/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

object PlatformSpecialized : TemplateGroupBase() {
    init {
        defaultBuilder {
            jvmOnly = true
        }
    }

    val f_fill = fn("fill(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc { "Fills original array with the provided value." }
        returns("Unit")
        body {
            """
            java.util.Arrays.fill(this, fromIndex, toIndex, element)
            """
        }
    }

    val f_binarySearch = fn("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
    } builder {
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

    val f_binarySearch_comparator = fn("binarySearch(element: T, comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
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


    val f_sort = fn("sort()") {
        // left with more generic signature for JVM only
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
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

    val f_sort_range = fn("sort(fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
    } builder {
        doc { "Sorts a range in the array in-place." }
        returns("Unit")
        body {
            "java.util.Arrays.sort(this, fromIndex, toIndex)"
        }
    }

    val f_sortWith = fn("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
        doc { "Sorts a range in the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "java.util.Arrays.sort(this, fromIndex, toIndex, comparator)"
        }
    }

    val f_filterIsInstanceTo = fn("filterIsInstanceTo(destination: C, klass: Class<R>)") {
        platforms(Platform.JVM)
        include(Iterables, ArraysOfObjects, Sequences)
    } builder {
        doc { "Appends all elements that are instances of specified class to the given [destination]." }
        receiverAsterisk = true
        typeParam("C : MutableCollection<in R>")
        typeParam("R")
        returns("C")
        body {
            """
            @Suppress("UNCHECKED_CAST")
            for (element in this) if (klass.isInstance(element)) destination.add(element as R)
            return destination
            """
        }
    }

    val f_filterIsInstance = fn("filterIsInstance(klass: Class<R>)") {
        platforms(Platform.JVM)
        include(Iterables, ArraysOfObjects, Sequences)
    } builder {
        doc { "Returns a list containing all elements that are instances of specified class." }
        receiverAsterisk= true
        typeParam("R")
        returns("List<R>")
        body {
            """
            return filterIsInstanceTo(ArrayList<R>(), klass)
            """
        }

        specialFor(Sequences) {
            doc { "Returns a sequence containing all elements that are instances of specified class." }
            returns("Sequence<R>")
        }
        body(Sequences) {
            """
            @Suppress("UNCHECKED_CAST")
            return filter { klass.isInstance(it) } as Sequence<R>
            """
        }
    }
}

object PlatformSpecializedJS : TemplateGroupBase() {
    val f_sort = fn("sort(noinline comparison: (a: T, b: T) -> Int)") {
        platforms(Platform.JS)
        include(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
    } builder {
        inlineOnly()
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparison] function." }
        body { "asDynamic().sort(comparison)" }
    }
}

object CommonArrays : TemplateGroupBase() {

    val f_plusElement = fn("plusElement(element: T)") {
        include(InvariantArraysOfObjects)
    } builder {
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }

        on(Platform.JVM) {
            inlineOnly()
            body { "return plus(element)" }
        }
        on(Platform.JS) {
            family = ArraysOfObjects
            inline(suppressWarning = true)
            returns("Array<T>")
            body {
                """
                return this.asDynamic().concat(arrayOf(element))
                """
            }
        }
    }

    val f_plus = fn("plus(element: T)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
    } builderWith { primitive ->
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        operator()
        returns("SELF")

        on(Platform.JVM) {
            body {
                """
                val index = size
                val result = java.util.Arrays.copyOf(this, index + 1)
                result[index] = element
                return result
                """
            }
        }

        on(Platform.JS) {
            inline(suppressWarning = true)
            specialFor(InvariantArraysOfObjects) {
                family = ArraysOfObjects
                returns("Array<T>")
            }

            body {
                if (primitive == null)
                    "return this.asDynamic().concat(arrayOf(element))"
                else
                    "return plus(${primitive.name.toLowerCase()}ArrayOf(element))"
            }
        }
    }


    val f_plus_collection = fn("plus(elements: Collection<T>)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
    } builder {
        operator()
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }
        on(Platform.JVM) {
            body {
                """
                var index = size
                val result = java.util.Arrays.copyOf(this, index + elements.size)
                for (element in elements) result[index++] = element
                return result
                """
            }
        }
        on(Platform.JS) {
            // TODO: inline arrayPlusCollection when @PublishedAPI is available
//                    inline(Platform.JS, Inline.Yes)
//                    annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")
            specialFor(InvariantArraysOfObjects) {
                family = ArraysOfObjects
                returns("Array<T>")
            }
            when (primitive) {
                null, PrimitiveType.Boolean, PrimitiveType.Long ->
                    body { "return arrayPlusCollection(this, elements)" }
                else ->
                    body { "return fillFromCollection(this.copyOf(size + elements.size), this.size, elements)" }
            }
        }
    }

    val f_plus_array = fn("plus(elements: SELF)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
    } builder {
        operator(true)
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        returns("SELF")
        specialFor(InvariantArraysOfObjects) {
            signature("plus(elements: Array<out T>)", notForSorting = true)
        }

        on(Platform.JVM) {
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
        on(Platform.JS) {
            inline(suppressWarning = true)
            specialFor(InvariantArraysOfObjects) {
                family = ArraysOfObjects
                returns("Array<T>")
                body { """return this.asDynamic().concat(elements)""" }
            }
            specialFor(ArraysOfPrimitives) {
                body { """return primitiveArrayConcat(this, elements)""" }
            }
        }
    }


    val f_copyOfRange = fn("copyOfRange(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
    } builderWith { primitive ->
        doc { "Returns new array which is a copy of range of original array." }
        returns("SELF")

        on(Platform.JS) {
            specialFor(InvariantArraysOfObjects) {
                family = ArraysOfObjects
                returns("Array<T>")
            }
            when(primitive) {
                PrimitiveType.Char, PrimitiveType.Boolean, PrimitiveType.Long ->
                    body { "return withType(\"${primitive}Array\", this.asDynamic().slice(fromIndex, toIndex))" }
                else -> {
                    inline(suppressWarning = true)
                    body { "return this.asDynamic().slice(fromIndex, toIndex)" }
                }
            }
        }
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.copyOfRange(this, fromIndex, toIndex)" }
        }
    }

    val f_copyOf = fn("copyOf()") {
        include(InvariantArraysOfObjects)
        include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives)
    } builder {
        doc { "Returns new array which is a copy of the original array." }
        returns("SELF")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.copyOf(this, size)" }
        }
        on(Platform.JS) {
            specialFor(InvariantArraysOfObjects) {
                family = ArraysOfObjects
                returns("Array<T>")
            }
            when (primitive) {
                null -> {
                    inline(suppressWarning = true)
                    body { "return this.asDynamic().slice()" }
                }
                PrimitiveType.Char, PrimitiveType.Boolean, PrimitiveType.Long ->
                    body { "return withType(\"${primitive}Array\", this.asDynamic().slice())" }
                else -> {
                    inline(suppressWarning = true)
                    body { "return this.asDynamic().slice()" }
                }
            }
        }
    }

    val f_copyOf_newSize = fn("copyOf(newSize: Int)") {
        include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives)
        include(InvariantArraysOfObjects)
    } builder {
        doc { "Returns new array which is a copy of the original array, resized to the given [newSize]." }
        specialFor(ArraysOfPrimitives) {
            returns("SELF")
            on(Platform.JS) {
                when (primitive!!) {
                    PrimitiveType.Boolean ->
                        body { "return withType(\"BooleanArray\", arrayCopyResize(this, newSize, false))" }
                    PrimitiveType.Char ->
                        body { "return withType(\"CharArray\", fillFrom(this, ${primitive}Array(newSize)))" }
                    PrimitiveType.Long ->
                        body { "return withType(\"LongArray\", arrayCopyResize(this, newSize, ZERO))" }
                    else ->
                        body { "return fillFrom(this, ${primitive}Array(newSize))" }
                }
            }

        }
        specialFor(InvariantArraysOfObjects) {
            returns("Array<T?>")
            on(Platform.JS) {
                family = ArraysOfObjects
                body { "return arrayCopyResize(this, newSize, null)" }
            }
        }
        on(Platform.JVM) {
            inlineOnly()
            body {
                "return java.util.Arrays.copyOf(this, newSize)"
            }
        }
    }

    fun f_sort() = fn("sort()") {
        include(ArraysOfPrimitives, PrimitiveType.numericPrimitives + PrimitiveType.Char)
        include(ArraysOfObjects)
    } builder {
        typeParam("T: Comparable<T>")
        doc { "Sorts the array in-place according to the natural order of its elements." }
        specialFor(ArraysOfPrimitives) {
            doc { "Sorts the array in-place." }
        }

        returns("Unit")
        on(Platform.JS) {
            body {
                """
                if (size > 1)
                    sort { a: T, b: T -> a.compareTo(b) }
                """
            }
            specialFor(ArraysOfPrimitives) {
                if (primitive != PrimitiveType.Long) {
                    annotation("""@library("primitiveArraySort")""")
                    body { "definedExternally" }
                }
            }
        }
        on(Platform.JVM) {
            specialFor(ArraysOfObjects) {
                inlineOnly()
                body {
                    """
                    @Suppress("UNCHECKED_CAST")
                    (this as Array<Any?>).sort()
                    """
                }
            }
            specialFor(ArraysOfPrimitives) {
                body {
                    "if (size > 1) java.util.Arrays.sort(this)"
                }
            }
        }
    }

    fun f_sortWith() = fn("sortWith(comparator: Comparator<in T>)") {
        include(ArraysOfObjects)
    } builder {
        doc { "Sorts the array in-place according to the order specified by the given [comparator]." }
        returns("Unit")
        on(Platform.JVM) {
            body {
                "if (size > 1) java.util.Arrays.sort(this, comparator)"
            }
        }
        on(Platform.JS) {
            body {
                """
                if (size > 1)
                    sort { a, b -> comparator.compare(a, b) }
                """
            }
        }
    }

    fun f_asList() = fn("asList()") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        on(Platform.JVM) {
            body { """return ArraysUtilJVM.asList(this)""" }
        }
        on(Platform.JS) {
            body { """return ArrayList<T>(this.unsafeCast<Array<Any?>>())""" }
        }

        specialFor(ArraysOfPrimitives) {
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
            on(Platform.JVM) {
                body { objectLiteralImpl }
            }
            on(Platform.JS) {
                if (primitive == PrimitiveType.Char) {
                    body { objectLiteralImpl }
                }
                else {
                    inlineOnly()
                    body { "return this.unsafeCast<Array<T>>().asList()" }
                }
            }
        }
    }

    fun f_toTypedArray() = fn("toTypedArray()") {
        include(ArraysOfPrimitives)
    } builder {
        returns("Array<T>")
        doc {
            """
            Returns a *typed* object array containing all of the elements of this primitive array.
            """
        }
        on(Platform.JVM) {
            body {
                """
                val result = arrayOfNulls<T>(size)
                for (index in indices)
                    result[index] = this[index]
                @Suppress("UNCHECKED_CAST")
                return result as Array<T>
                """
            }
        }
        on(Platform.JS) {
            when (primitive) {
                PrimitiveType.Char ->
                    body { "return Array<Char>(size, { i -> this[i] })" }
                PrimitiveType.Boolean, PrimitiveType.Long ->
                    body { "return copyOf().unsafeCast<Array<T>>()" }
                else ->
                    body { "return js(\"[]\").slice.call(this)" }
            }

        }
    }
}
