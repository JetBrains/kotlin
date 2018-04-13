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

object ArrayOps : TemplateGroupBase() {

    val f_isEmpty = fn("isEmpty()") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        inlineOnly()
        doc { "Returns `true` if the array is empty." }
        returns("Boolean")
        body {
            "return size == 0"
        }
    }

    val f_isNotEmpty = fn("isNotEmpty()") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        inlineOnly()
        doc { "Returns `true` if the array is not empty." }
        returns("Boolean")
        body {
            "return !isEmpty()"
        }
    }


    val f_lastIndex = pval("lastIndex") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc { "Returns the last valid index for the array." }
        returns("Int")
        body {
            "get() = size - 1"
        }
    }

    val f_indices = pval("indices") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        doc { "Returns the range of valid indices for the array." }
        returns("IntRange")
        body {
            "get() = IntRange(0, lastIndex)"
        }
    }

    val f_contentEquals = fn("contentEquals(other: SELF)") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        since("1.1")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *structurally* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            """
        }
        returns("Boolean")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.equals(this, other)" }
        }

        on(Platform.JS) {
            annotation("""@library("arrayEquals")""")
            body { "definedExternally" }
        }
    }

    val f_contentDeepEquals = fn("contentDeepEquals(other: SELF)") {
        include(ArraysOfObjects)
    } builder {
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
        returns("Boolean")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.deepEquals(this, other)" }
        }
        on(Platform.JS) {
            annotation("""@library("arrayDeepEquals")""")
            body { "definedExternally" }
        }
    }

    val f_contentToString = fn("contentToString()") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        since("1.1")
        doc {
            """
            Returns a string representation of the contents of the specified array as if it is [List].

            @sample samples.collections.Arrays.ContentOperations.contentToString
            """
        }
        returns("String")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.toString(this)" }
        }
        on(Platform.JS) {
            annotation("""@library("arrayToString")""")
            body { "definedExternally" }
        }
    }

    val f_contentDeepToString = fn("contentDeepToString()") {
        include(ArraysOfObjects)
    } builder {
        since("1.1")
        doc {
            """
            Returns a string representation of the contents of this array as if it is a [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level that reference
            is rendered as `"[...]"` to prevent recursion.

            @sample samples.collections.Arrays.ContentOperations.contentDeepToString
            """
        }
        returns("String")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.deepToString(this)" }
        }
        on(Platform.JS) {
            annotation("""@library("arrayDeepToString")""")
            body { "definedExternally" }
        }
    }

    val f_contentHashCode = fn("contentHashCode()") {
        include(ArraysOfObjects, ArraysOfPrimitives)
    } builder {
        since("1.1")
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        returns("Int")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.hashCode(this)" }
        }
        on(Platform.JS) {
            annotation("""@library("arrayHashCode")""")
            body { "definedExternally" }
        }
    }

    val f_contentDeepHashCode = fn("contentDeepHashCode()") {
        include(ArraysOfObjects)
    } builder {
        since("1.1")
        doc {
            """
            Returns a hash code based on the contents of this array as if it is [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        returns("Int")
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.deepHashCode(this)" }
        }
        on(Platform.JS) {
            annotation("""@library("arrayDeepHashCode")""")
            body { "definedExternally" }
        }
    }

    val f_toPrimitiveArray = fn("toPrimitiveArray()") {
        include(ArraysOfObjects, PrimitiveType.defaultPrimitives)
        include(Collections, PrimitiveType.defaultPrimitives)
    } builder {
        val primitive = checkNotNull(primitive)
        val arrayType = primitive.name + "Array"
        signature("to$arrayType()")
        returns(arrayType)
        // TODO: Use different implementations for JS
        specialFor(ArraysOfObjects) {
            doc { "Returns an array of ${primitive.name} containing all of the elements of this generic array." }
            body {
                """
                val result = $arrayType(size)
                for (index in indices)
                    result[index] = this[index]
                return result
                """
            }
        }
        specialFor(Collections) {
            doc { "Returns an array of ${primitive.name} containing all of the elements of this collection." }
            body {
                """
                val result = $arrayType(size)
                var index = 0
                for (element in this)
                    result[index++] = element
                return result
                """
            }
        }
    }

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
            suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
            returns("Array<T>")
            body {
                """
                return this.asDynamic().concat(arrayOf(element))
                """
            }
        }
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                returns("Array<T>")
            }

            body {
                if (primitive == null)
                    "return this.asDynamic().concat(arrayOf(element))"
                else
                    "return plus(${primitive.name.toLowerCase()}ArrayOf(element))"
            }
        }
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                returns("Array<T>")
            }
            when (primitive) {
                null, PrimitiveType.Boolean, PrimitiveType.Long ->
                    body { "return arrayPlusCollection(this, elements)" }
                else ->
                    body { "return fillFromCollection(this.copyOf(size + elements.size), this.size, elements)" }
            }
        }
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                returns("Array<T>")
                body { """return this.asDynamic().concat(elements)""" }
            }
            specialFor(ArraysOfPrimitives) {
                body { """return primitiveArrayConcat(this, elements)""" }
            }
        }
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
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
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
            }
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
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
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
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
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                body { "return arrayCopyResize(this, newSize, null)" }
            }
        }
        on(Platform.JVM) {
            inlineOnly()
            body {
                "return java.util.Arrays.copyOf(this, newSize)"
            }
        }
        on(Platform.Common) {
            specialFor(InvariantArraysOfObjects) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
            }
        }
    }

    val f_sort = fn("sort()") {
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

    val f_sortWith = fn("sortWith(comparator: Comparator<in T>)") {
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

    val f_sort_comparison = fn("sort(noinline comparison: (a: T, b: T) -> Int)") {
        platforms(Platform.JS)
        include(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
    } builder {
        inlineOnly()
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparison] function." }
        body { "asDynamic().sort(comparison)" }
    }

    val f_sort_objects = fn("sort()") {
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

    val f_sortWith_range = fn("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
        doc { "Sorts a range in the array in-place with the given [comparator]." }
        returns("Unit")
        body {
            "java.util.Arrays.sort(this, fromIndex, toIndex, comparator)"
        }
    }



    val f_asList = fn("asList()") {
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

    val f_toTypedArray = fn("toTypedArray()") {
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
}
