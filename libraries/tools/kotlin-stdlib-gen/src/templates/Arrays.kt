/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.Family.*

object ArrayOps : TemplateGroupBase() {

    init {
        defaultBuilder {
            specialFor(ArraysOfUnsigned) {
                since("1.3")
                annotation("@ExperimentalUnsignedTypes")
            }
        }
    }

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
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
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
        if (family == ArraysOfUnsigned) {
            body { "return storage.contentEquals(other.storage)" }
            return@builder
        }
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
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.1")
        doc {
            """
            Returns a string representation of the contents of the specified array as if it is [List].
            """
        }
        sample("samples.collections.Arrays.ContentOperations.contentToString")
        returns("String")
        if (family == ArraysOfUnsigned) {
            body { """return joinToString(", ", "[", "]")""" }
            return@builder
        }
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
            """
        }
        sample("samples.collections.Arrays.ContentOperations.contentDeepToString")
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
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.1")
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        returns("Int")
        if (family == ArraysOfUnsigned) {
            body { "return storage.contentHashCode()" }
            return@builder
        }
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

    val f_asSignedArray = fn("asSignedArray()") {
        include(ArraysOfUnsigned)
    } builder {
        val arrayType = primitive!!.name.drop(1) + "Array"
        signature("as$arrayType()")
        returns(arrayType)

        doc {
            """
            Returns an array of type [$arrayType], which is a view of this array where each element is a signed reinterpretation
            of the corresponding element of this array.
            """
        }

        inlineOnly()
        body { """return storage""" }
    }

    val f_toSignedArray = fn("toSignedArray()") {
        include(ArraysOfUnsigned)
    } builder {
        val arrayType = primitive!!.name.drop(1) + "Array"
        signature("to$arrayType()")
        returns(arrayType)

        doc {
            """
            Returns an array of type [$arrayType], which is a copy of this array where each element is a signed reinterpretation
            of the corresponding element of this array.
            """
        }

        inlineOnly()
        body { """return storage.copyOf()""" }
    }

    val f_asUnsignedArray = fn("asUnsignedArray()") {
        include(ArraysOfUnsigned)
    } builder {
        val arrayType = primitive!!.name.drop(1) + "Array"
        receiver(arrayType)
        signature("asU$arrayType()")
        returns("SELF")

        doc {
            """
            Returns an array of type [U$arrayType], which is a view of this array where each element is an unsigned reinterpretation
            of the corresponding element of this array.
            """
        }

        inlineOnly()
        body { """return U$arrayType(this)""" }
    }

    val f_toUnsignedArray = fn("toUnsignedArray()") {
        include(ArraysOfUnsigned)
    } builder {
        val arrayType = primitive!!.name.drop(1) + "Array"
        receiver(arrayType)
        signature("toU$arrayType()")
        returns("SELF")

        doc {
            """
            Returns an array of type [U$arrayType], which is a copy of this array where each element is an unsigned reinterpretation
            of the corresponding element of this array.
            """
        }

        inlineOnly()
        body { """return U$arrayType(this.copyOf())""" }
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

    // TODO: Remove -1 from common signature
    val f_copyInto = fn("copyInto(destination: SELF, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = -1)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.3")
        returns("SELF")

        doc {
            """
            Copies this array or its subrange into the [destination] array and returns that array.

            It's allowed to pass the same array in the [destination] and even specify the subrange so that it overlaps with the destination range.

            @param destination the array to copy to.
            @param destinationOffset the position in the [destination] array to copy to, 0 by default.
            @param startIndex the beginning (inclusive) of the subrange to copy, 0 by default.
            @param endIndex the end (exclusive) of the subrange to copy, size of this array by default.

            @throws IndexOutOfBoundsException or [IllegalArgumentException] when [startIndex] or [endIndex] is out of range of this array indices or when `startIndex > endIndex`.
            @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationIndex],
            or when that index is out of the [destination] array indices range.

            @return the [destination] array.
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body {
                "return SELF(storage.copyInto(destination.storage, destinationOffset, startIndex, endIndex))"
            }
        }
        specialFor(ArraysOfPrimitives, InvariantArraysOfObjects) {
            specialFor(InvariantArraysOfObjects) {
                receiver("Array<out T>")
            }
            on(Platform.JVM) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                signature("copyInto(destination: SELF, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size)")
                body {
                    """
                    @Suppress("NAME_SHADOWING")
                    val endIndex = if (endIndex == -1) size else endIndex // TODO: Remove when default value from expect is fixed
                    System.arraycopy(this, startIndex, destination, destinationOffset, endIndex - startIndex)
                    return destination
                    """
                }
            }
            on(Platform.JS) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                signature("copyInto(destination: SELF, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size)")
                inlineOnly()
                body {
                    val cast = ".unsafeCast<Array<$primitive>>()".takeIf { family == ArraysOfPrimitives } ?: ""
                    """
                    arrayCopy(this$cast, destination$cast, destinationOffset, startIndex, endIndex)
                    return destination
                    """
                }
            }
        }
    }

    val f_copyOfRangeJvmImpl = fn("copyOfRangeImpl(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
        platforms(Platform.JVM)
    } builderWith { primitive ->
        since("1.3")
        visibility("internal")
        annotation("@PublishedApi")
        annotation("""@JvmName("copyOfRange")""")
        returns("SELF")
        body {
            """
            copyOfRangeToIndexCheck(toIndex, size)
            return java.util.Arrays.copyOfRange(this, fromIndex, toIndex)
            """
        }
    }

    val f_copyOfRange = fn("copyOfRange(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builderWith { primitive ->
        doc {
            """
            Returns a new array which is a copy of the specified range of the original array.

            @param fromIndex the start of the range (inclusive), must be in `0..array.size`
            @param toIndex the end of the range (exclusive), must be in `fromIndex..array.size`
            """
        }
        returns("SELF")

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return SELF(storage.copyOfRange(fromIndex, toIndex))" }
        }
        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
            on(Platform.JS) {
                specialFor(InvariantArraysOfObjects) {
                    family = ArraysOfObjects
                    suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                    returns("Array<T>")
                }
                val rangeCheck = "AbstractList.checkRangeIndexes(fromIndex, toIndex, size)"
                when (primitive) {
                    PrimitiveType.Char, PrimitiveType.Boolean, PrimitiveType.Long ->
                        body { "return withType(\"${primitive}Array\", this.asDynamic().slice(fromIndex, toIndex))" }
                    else -> {
                        body { "return this.asDynamic().slice(fromIndex, toIndex)" }
                    }
                }
                body { rangeCheck + "\n" + body }
            }
            on(Platform.JVM) {
                annotation("""@JvmName("copyOfRangeInline")""")
                inlineOnly()
                body {
                    """
                    return if (kotlin.internal.apiVersionIsAtLeast(1, 3, 0)) {
                        copyOfRangeImpl(fromIndex, toIndex)
                    } else {
                        if (toIndex > size) throw IndexOutOfBoundsException("toIndex: ${'$'}toIndex, size: ${'$'}size")
                        java.util.Arrays.copyOfRange(this, fromIndex, toIndex)
                    }
                    """
                }
            }
            on(Platform.Common) {
                specialFor(InvariantArraysOfObjects) {
                    suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
                }
            }
        }
    }

    val f_copyOf = fn("copyOf()") {
        include(InvariantArraysOfObjects)
        include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives)
        include(ArraysOfUnsigned)
    } builder {
        doc { "Returns new array which is a copy of the original array." }
        sample("samples.collections.Arrays.CopyOfOperations.copyOf")
        returns("SELF")

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return SELF(storage.copyOf())" }
        }
        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
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
    }

    val f_copyOf_newSize = fn("copyOf(newSize: Int)") {
        include(ArraysOfPrimitives, PrimitiveType.defaultPrimitives)
        include(InvariantArraysOfObjects)
        include(ArraysOfUnsigned)
    } builder {
        doc {
            """
            Returns new array which is a copy of the original array, resized to the given [newSize].
            The copy is either truncated or padded at the end with ${primitive.zero} values if necessary.

            - If [newSize] is less than the size of the original array, the copy array is truncated to the [newSize].
            - If [newSize] is greater than the size of the original array, the extra elements in the copy array are filled with ${primitive.zero} values.
            """
        }
        val newSizeCheck = """require(newSize >= 0) { "Invalid new array size: ${'$'}newSize." }"""
        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            returns("SELF")
            body { "return SELF(storage.copyOf(newSize))" }
        }
        specialFor(ArraysOfPrimitives) {
            sample("samples.collections.Arrays.CopyOfOperations.resizedPrimitiveCopyOf")
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
                body { newSizeCheck + "\n" + body }
            }

        }
        specialFor(InvariantArraysOfObjects) {
            sample("samples.collections.Arrays.CopyOfOperations.resizingCopyOf")
            returns("Array<T?>")
            on(Platform.JS) {
                family = ArraysOfObjects
                suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                body {
                    """
                    $newSizeCheck
                    return arrayCopyResize(this, newSize, null)
                    """
                }
            }
            on(Platform.Common) {
                suppress("NO_ACTUAL_FOR_EXPECT") // TODO: KT-21937
            }
        }
        specialFor(ArraysOfPrimitives, InvariantArraysOfObjects) {
            on(Platform.JVM) {
                inlineOnly()
                body {
                    "return java.util.Arrays.copyOf(this, newSize)"
                }
            }
        }
    }

    val f_sort = fn("sort()") {
        include(ArraysOfPrimitives, PrimitiveType.numericPrimitives + PrimitiveType.Char)
        include(ArraysOfObjects)
    } builder {
        typeParam("T : Comparable<T>")
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
        include(ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        returns("Array<T>")
        doc {
            """
            Returns a *typed* object array containing all of the elements of this primitive array.
            """
        }
        body { "return Array(size) { index -> this[index] }" }
        specialFor(ArraysOfPrimitives) {
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
                    PrimitiveType.Char -> {}
                    PrimitiveType.Boolean, PrimitiveType.Long ->
                        body { "return copyOf().unsafeCast<Array<T>>()" }
                    else ->
                        body { "return js(\"[]\").slice.call(this)" }
                }

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
