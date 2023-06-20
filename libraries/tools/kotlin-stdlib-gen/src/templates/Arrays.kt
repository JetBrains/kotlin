 /*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package templates

import templates.DocExtensions.collection
import templates.Family.*
import templates.Ordering.appendStableSortNote
import templates.Ordering.stableSortNote

object ArrayOps : TemplateGroupBase() {

    init {
        defaultBuilder {
            specialFor(ArraysOfUnsigned) {
                sinceAtLeast("1.3")
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
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns the last valid index for the array." }
        returns("Int")
        body {
            "get() = size - 1"
        }
        specialFor(ArraysOfUnsigned) {
            // TODO: Make inlineOnly after KT-30015 is fixed.
            // InlineOnly properties currently are not inlined and may lead to IllegalAccessException
            // when accessed from an inline (or inlineOnly) method.
            // It is because the method body contains access call to inlineOnly property in nonpublic multifile part,
            // which may be inaccessible from method call site, where method body gets inlined.
            inline()
            body { "get() = storage.lastIndex" }
        }
    }

    val f_indices = pval("indices") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns the range of valid indices for the array." }
        returns("IntRange")
        body {
            "get() = IntRange(0, lastIndex)"
        }
        specialFor(ArraysOfUnsigned) {
            // TODO: Make inlineOnly after KT-30015 is fixed.
            // InlineOnly properties currently are not inlined and may lead to IllegalAccessException
            // when accessed from an inline (or inlineOnly) method.
            // It is because the method body contains access call to inlineOnly property in nonpublic multifile part,
            // which may be inaccessible from method call site, where method body gets inlined.
            inline()
            body { "get() = storage.indices" }
        }
    }

    private fun MemberBuilder.deprecatedNonNullArrayFunction() {
        deprecate("Use Kotlin compiler 1.4 to avoid deprecation warning.")
        annotation("""@DeprecatedSinceKotlin(hiddenSince = "1.4")""")
    }

    val f_contentEquals = fn("contentEquals(other: SELF)") {
        platforms(Platform.Native)
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.1")
        deprecatedNonNullArrayFunction()
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *structurally* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            """
        }
        returns("Boolean")
        body { "return this.contentEquals(other)" }
        if (f == ArraysOfUnsigned) {
            return@builder
        }
        doc {
            doc + """
            The elements are compared for equality with the [equals][Any.equals] function.
            For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
            """
        }
    }

    val f_contentEquals_nullable = fn("contentEquals(other: SELF?)") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *structurally* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            """
        }
        receiver("SELF?")
        returns("Boolean")
        if (family == ArraysOfUnsigned) {
            body { "return this?.storage.contentEquals(other?.storage)" }
            return@builder
        }

        doc {
            doc + """
            The elements are compared for equality with the [equals][Any.equals] function.
            For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
            """
        }
        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.equals(this, other)" }
        }

        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayEquals")""")
                body { "definedExternally" }
            }

            on(Backend.IR) {
                body { "return contentEqualsInternal(other)" }
            }
        }
        on(Platform.Native) {
            fun notEq(operand1: String, operand2: String) = when {
                primitive?.isFloatingPoint() == true -> "!$operand1.equals($operand2)"
                else -> "$operand1 != $operand2"
            }
            body {
                """
                if (this === other) return true
                if (this === null || other === null) return false
                if (size != other.size) return false
                for (i in indices) {
                    if (${notEq("this[i]", "other[i]")}) return false
                }
                return true
                """
            }
        }
    }

    val f_contentDeepEquals = fn("contentDeepEquals(other: SELF)") {
        include(ArraysOfObjects)
    } builder {
        since("1.1")
        annotation("@kotlin.internal.LowPriorityInOverloadResolution")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *deeply* equal to one another,
            i.e. contain the same number of the same elements in the same order.

            If two corresponding elements are nested arrays, they are also compared deeply.
            If any of arrays contains itself on any nesting level the behavior is undefined.

            The elements of other types are compared for equality with the [equals][Any.equals] function.
            For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
            """
        }
        returns("Boolean")
        body { "return this.contentDeepEquals(other)" }
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepEqualsInline")""")
        }
    }

    val f_contentDeepEquals_nullable = fn("contentDeepEquals(other: SELF?)") {
        include(ArraysOfObjects)
    } builder {
        since("1.4")
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *deeply* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            
            The specified arrays are also considered deeply equal if both are `null`.

            If two corresponding elements are nested arrays, they are also compared deeply.
            If any of arrays contains itself on any nesting level the behavior is undefined.

            The elements of other types are compared for equality with the [equals][Any.equals] function.
            For floating point numbers it means that `NaN` is equal to itself and `-0.0` is not equal to `0.0`.
            """
        }
        receiver("SELF?")
        returns("Boolean")
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepEqualsNullable")""")
            body {
                """
                if (kotlin.internal.apiVersionIsAtLeast(1, 3, 0))
                    return contentDeepEqualsImpl(other)
                else
                    return java.util.Arrays.deepEquals(this, other)
                """
            }
        }
        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayDeepEquals")""")
                body { "definedExternally" }
            }

            on(Backend.IR) {
                body { "return contentDeepEqualsImpl(other)" }
            }
        }
        on(Platform.Native) {
            body { "return contentDeepEqualsImpl(other)" }
        }
    }

    val f_contentToString = fn("contentToString()") {
        platforms(Platform.Native)
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.1")
        deprecatedNonNullArrayFunction()
        doc {
            """
            Returns a string representation of the contents of the specified array as if it is [List].
            """
        }
        sample("samples.collections.Arrays.ContentOperations.contentToString")
        returns("String")
        body { "return this.contentToString()" }
        if (f == ArraysOfUnsigned) {
            return@builder
        }
    }

    val f_contentToString_nullable = fn("contentToString()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        doc {
            """
            Returns a string representation of the contents of the specified array as if it is [List].
            """
        }
        sample("samples.collections.Arrays.ContentOperations.contentToString")
        receiver("SELF?")
        returns("String")
        if (family == ArraysOfUnsigned) {
            body { """return this?.joinToString(", ", "[", "]") ?: "null"""" }
            return@builder
        }

        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.toString(this)" }
        }
        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayToString")""")
                body { "definedExternally" }
            }
            on(Backend.IR) {
                body { """return this?.joinToString(", ", "[", "]") ?: "null"""" }
            }
        }
        on(Platform.Native) {
            body { """return this?.joinToString(", ", "[", "]") ?: "null"""" }
        }
    }

    val f_contentDeepToString = fn("contentDeepToString()") {
        include(ArraysOfObjects)
    } builder {
        since("1.1")
        annotation("@kotlin.internal.LowPriorityInOverloadResolution")
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
        body { "return this.contentDeepToString()" }
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepToStringInline")""")
        }
    }

    val f_contentDeepToString_nullable = fn("contentDeepToString()") {
        include(ArraysOfObjects)
    } builder {
        since("1.4")
        doc {
            """
            Returns a string representation of the contents of this array as if it is a [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level that reference
            is rendered as `"[...]"` to prevent recursion.
            """
        }
        sample("samples.collections.Arrays.ContentOperations.contentDeepToString")
        receiver("SELF?")
        returns("String")
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepToStringNullable")""")
            body {
                """
                if (kotlin.internal.apiVersionIsAtLeast(1, 3, 0))
                    return contentDeepToStringImpl()
                else
                    return java.util.Arrays.deepToString(this)
                """
            }
        }
        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayDeepToString")""")
                body { "definedExternally" }
            }
            on(Backend.IR) {
                body { "return contentDeepToStringImpl()" }
            }
        }
        on(Platform.Native) {
            body { "return contentDeepToStringImpl()" }
        }
    }

    val f_contentHashCode = fn("contentHashCode()") {
        platforms(Platform.Native)
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.1")
        deprecatedNonNullArrayFunction()
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        returns("Int")
        body { "return this.contentHashCode()" }
        if (f == ArraysOfUnsigned) {
            return@builder
        }
    }

    val f_contentHashCode_nullable = fn("contentHashCode()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        since("1.4")
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        receiver("SELF?")
        returns("Int")
        if (family == ArraysOfUnsigned) {
            body { "return this?.storage.contentHashCode()" }
            return@builder
        }

        on(Platform.JVM) {
            inlineOnly()
            body { "return java.util.Arrays.hashCode(this)" }
        }
        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayHashCode")""")
                body { "definedExternally" }
            }
            on(Backend.IR) {
                body { "return contentHashCodeInternal()" }
            }
        }
        on(Platform.Native) {
            body {
                """
                if (this === null) return 0
                var result = 1
                for (element in this)
                    result = 31 * result + element.hashCode()
                return result
                """
            }
        }
    }

    val f_contentDeepHashCode = fn("contentDeepHashCode()") {
        include(ArraysOfObjects)
    } builder {
        since("1.1")
        annotation("@kotlin.internal.LowPriorityInOverloadResolution")
        doc {
            """
            Returns a hash code based on the contents of this array as if it is [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        returns("Int")
        body { "return this.contentDeepHashCode()" }
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepHashCodeInline")""")
        }
    }

    val f_contentDeepHashCode_nullable = fn("contentDeepHashCode()") {
        include(ArraysOfObjects)
    } builder {
        since("1.4")
        doc {
            """
            Returns a hash code based on the contents of this array as if it is [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        receiver("SELF?")
        returns("Int")
        on(Platform.JVM) {
            inlineOnly()
            annotation("""@JvmName("contentDeepHashCodeNullable")""")
            body {
                """
                if (kotlin.internal.apiVersionIsAtLeast(1, 3, 0))
                    return contentDeepHashCodeImpl()
                else
                    return java.util.Arrays.deepHashCode(this)
                """
            }
        }
        on(Platform.JS) {
            on(Backend.Legacy) {
                annotation("""@library("arrayDeepHashCode")""")
                body { "definedExternally" }
            }
            on(Backend.IR) {
                body { "return contentDeepHashCodeInternal()" }
            }
        }
        on(Platform.Native) {
            body { "return contentDeepHashCodeImpl()" }
        }
    }

    val f_toPrimitiveArray = fn("toPrimitiveArray()") {
        include(ArraysOfObjects, PrimitiveType.values().toSet())
        include(Collections, PrimitiveType.values().toSet())
    } builder {
        val primitive = checkNotNull(primitive)
        val arrayType = primitive.name + "Array"
        signature("to$arrayType()")
        returns(arrayType)

        if (primitive in PrimitiveType.unsignedPrimitives) {
            since("1.3")
            annotation("@ExperimentalUnsignedTypes")
        }

        // TODO: Use different implementations for JS
        specialFor(ArraysOfObjects) {
            if (primitive in PrimitiveType.unsignedPrimitives) {
                sourceFile(SourceFile.UArrays)
            }
            doc { "Returns an array of ${primitive.name} containing all of the elements of this generic array." }
            body {
                """
                return $arrayType(size) { index -> this[index] }
                """
            }
        }
        specialFor(Collections) {
            if (primitive in PrimitiveType.unsignedPrimitives) {
                sourceFile(SourceFile.UCollections)
            }
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
        on(Platform.Native) {
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
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builderWith { primitive ->
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        operator()
        returns("SELF")

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            val signedPrimitiveName = primitive!!.name.drop(1)
            body {
                """
                return SELF(storage + element.to$signedPrimitiveName())
                """
            }
        }

        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
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
                        "return plus(${primitive.name.lowercase()}ArrayOf(element))"
                }
            }
            on(Platform.Native) {
                body {
                    """
                    val index = size
                    val result = copyOfUninitializedElements(index + 1)
                    result[index] = element
                    return result
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


    val f_plus_collection = fn("plus(elements: Collection<T>)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        operator()
        returns("SELF")
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] collection." }

        specialFor(ArraysOfUnsigned) {
            val signedPrimitiveName = primitive!!.name.drop(1)
            body {
                """
                var index = size
                val result = storage.copyOf(size + elements.size)
                for (element in elements) result[index++] = element.to$signedPrimitiveName()
                return SELF(result)
                """
            }
        }
        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
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
//                        inline(Platform.JS, Inline.Yes)
//                        annotations(Platform.JS, """@Suppress("NOTHING_TO_INLINE")""")
                specialFor(InvariantArraysOfObjects) {
                    family = ArraysOfObjects
                    suppress("ACTUAL_WITHOUT_EXPECT") // TODO: KT-21937
                    returns("Array<T>")
                }
                when (primitive) {
                    null, PrimitiveType.Boolean, PrimitiveType.Long ->
                        body { "return arrayPlusCollection(this, elements)" }
                    else -> {
                        on(Backend.Legacy) {
                            body {
                                "return fillFromCollection(this.copyOf(size + elements.size), this.size, elements)"
                            }
                        }
                        on(Backend.IR) {
                            // Don't use fillFromCollection because it treats arrays
                            // as `dynamic` but we need to concrete types to perform
                            // unboxing of collections elements
                            body {
                                """
                                var index = size
                                val result = this.copyOf(size + elements.size)
                                for (element in elements) result[index++] = element
                                return result
                                """
                            }
                        }
                    }
                }
            }
            on(Platform.Native) {
                body {
                    """
                    var index = size
                    val result = copyOfUninitializedElements(index + elements.size)
                    for (element in elements) result[index++] = element
                    return result
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

    val f_plus_array = fn("plus(elements: SELF)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        operator(true)
        doc { "Returns an array containing all elements of the original array and then all elements of the given [elements] array." }
        returns("SELF")

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body { "return SELF(storage + elements.storage)" }
        }
        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
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
            on(Platform.Native) {
                body {
                    """
                    val thisSize = size
                    val arraySize = elements.size
                    val result = copyOfUninitializedElements(thisSize + arraySize)
                    elements.copyInto(result, thisSize)
                    return result
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

    val f_copyInto = fn("copyInto(destination: SELF, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = size)") {
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
            @throws IndexOutOfBoundsException when the subrange doesn't fit into the [destination] array starting at the specified [destinationOffset],
            or when that index is out of the [destination] array indices range.

            @return the [destination] array.
            """
        }

        specialFor(ArraysOfUnsigned) {
            inlineOnly()
            body {
                """
                storage.copyInto(destination.storage, destinationOffset, startIndex, endIndex)
                return destination
                """
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
            on(Platform.Native) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                body {
                    """
                    arrayCopy(this, startIndex, destination, destinationOffset, endIndex - startIndex)
                    return destination
                    """
                }
                specialFor(InvariantArraysOfObjects) {
                    body {
                        """
                        @Suppress("UNCHECKED_CAST")
                        arrayCopy(this as Array<Any?>, startIndex, destination as Array<Any?>, destinationOffset, endIndex - startIndex)
                        return destination
                        """
                    }
                }
                on(Backend.Wasm) {
                    body {
                        """
                        AbstractList.checkRangeIndexes(startIndex, endIndex, this.size)
                        val rangeSize = endIndex - startIndex
                        AbstractList.checkRangeIndexes(destinationOffset, destinationOffset + rangeSize, destination.size)
                        kotlin.wasm.internal.copyWasmArray(this.storage, destination.storage, startIndex, destinationOffset, rangeSize)
                        return destination
                        """
                    }
                }
            }
        }
    }

    val f_copyOfRangeJvmImpl = fn("copyOfRangeImpl(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives)
        platforms(Platform.JVM)
    } builderWith { _ ->
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
            
            ${rangeDoc(hasDefault = false, action = "copy")}
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
            on(Platform.Native) {
                body {
                    """
                    checkCopyOfRangeArguments(fromIndex, toIndex, size)
                    return copyOfUninitializedElements(fromIndex, toIndex)
                    """
                }
            }
        }
    }

    val f_copyOfUninitializedElements_size = fn("copyOfUninitializedElements(newSize: Int)") {
        include(InvariantArraysOfObjects)
        include(ArraysOfPrimitives)
        platforms(Platform.Native)
    } builder {
        visibility("internal")
        returns("SELF")
        doc {
            """
            Returns new array which is a copy of the original array with new elements filled with **lateinit** _uninitialized_ values.
            Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
            either throwing exception or returning some kind of implementation-specific default value.
            """
        }
        body { "return copyOfUninitializedElements(0, newSize)" }
    }

    val f_copyOfUninitializedElements_range = fn("copyOfUninitializedElements(fromIndex: Int, toIndex: Int)") {
        include(InvariantArraysOfObjects)
        include(ArraysOfPrimitives)
        platforms(Platform.Native)
    } builder {
        visibility("internal")
        returns("SELF")
        doc {
            """
            Returns new array which is a copy of the original array's range between [fromIndex] (inclusive)
            and [toIndex] (exclusive) with new elements filled with **lateinit** _uninitialized_ values.
            Attempts to read _uninitialized_ values from this array work in implementation-dependent manner,
            either throwing exception or returning some kind of implementation-specific default value.
            """
        }
        body {
            """
            val newSize = toIndex - fromIndex
            if (newSize < 0) {
                throw IllegalArgumentException("${'$'}fromIndex > ${'$'}toIndex")
            }
            val result = ${if (f == InvariantArraysOfObjects) "arrayOfUninitializedElements<T>" else "SELF"}(newSize)
            this.copyInto(result, 0, fromIndex, toIndex.coerceAtMost(size))
            return result
            """
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
            on(Platform.Native) {
                body { "return this.copyOfUninitializedElements(size)" }
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
                        body { "return withType(\"LongArray\", arrayCopyResize(this, newSize, ${primitive!!.zero()}))" }
                    else ->
                        body { "return fillFrom(this, ${primitive}Array(newSize))" }
                }
                body { newSizeCheck + "\n" + body }
            }
            on(Platform.Native) {
                body { "return this.copyOfUninitializedElements(newSize)" }
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
            on(Platform.Native) {
                body { "return this.copyOfNulls(newSize)" }
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
        include(ArraysOfUnsigned)
        include(ArraysOfObjects)
    } builder {
        typeParam("T : Comparable<T>")
        doc { "Sorts the array in-place according to the natural order of its elements." }
        appendStableSortNote()
        specialFor(ArraysOfObjects) {
            sample("samples.collections.Arrays.Sorting.sortArrayOfComparable")
        }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) {
            doc { "Sorts the array in-place." }
            sample("samples.collections.Arrays.Sorting.sortArray")
        }

        returns("Unit")

        body(ArraysOfUnsigned) {
            """if (size > 1) sortArray(this, 0, size)"""
        }

        specialFor(ArraysOfPrimitives, ArraysOfObjects) {
            on(Platform.JS) {
                body {
                    """if (size > 1) sortArray(this)"""
                }
                specialFor(ArraysOfPrimitives) {
                    if (primitive != PrimitiveType.Long) {
                        on(Backend.Legacy) {
                            annotation("""@library("primitiveArraySort")""")
                            body { "definedExternally" }
                        }
                        on(Backend.IR) {
                            if (primitive == PrimitiveType.Char) {
                                // Requires comparator because default comparator of 'Array.prototype.sort' compares
                                // string representation of values
                                body { "nativeSort(::primitiveCompareTo)" }
                            } else {
                                body { "nativeSort()" }
                            }
                        }
                    } else {
                        body {
                            """
                            @Suppress("DEPRECATION")
                            if (size > 1) sort { a: T, b: T -> a.compareTo(b) }
                            """
                        }
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
            on(Platform.Native) {
                body { """if (size > 1) sortArray(this, 0, size)""" }
            }
        }
    }

    val f_sortWith = fn("sortWith(comparator: Comparator<in T>)") {
        include(ArraysOfObjects)
    } builder {
        doc { "Sorts the array in-place according to the order specified by the given [comparator]." }
        appendStableSortNote()
        returns("Unit")
        on(Platform.JVM) {
            body {
                "if (size > 1) java.util.Arrays.sort(this, comparator)"
            }
        }
        on(Platform.JS) {
            body {
                """if (size > 1) sortArrayWith(this, comparator)"""
            }
        }
        on(Platform.Native) {
            body { """if (size > 1) sortArrayWith(this, 0, size, comparator)""" }
        }
    }

    val f_sort_comparison = fn("sort(comparison: (a: T, b: T) -> Int)") {
        platforms(Platform.JS)
        include(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
    } builder {
        returns("Unit")
        doc { "Sorts the array in-place according to the order specified by the given [comparison] function." }
        specialFor(ArraysOfPrimitives) {
            deprecate(Deprecation("Use other sorting functions from the Standard Library", warningSince = "1.6"))
            inlineOnly()
            signature("sort(noinline comparison: (a: T, b: T) -> Int)")
            body { "nativeSort(comparison)" }
        }
        specialFor(ArraysOfObjects) {
            deprecate(
                Deprecation("Use sortWith instead", replaceWith = "this.sortWith(Comparator(comparison))", warningSince = "1.6")
            )
            appendStableSortNote()
            body { """if (size > 1) sortArrayWith(this, comparison)""" }
        }
    }

    val f_sort_objects = fn("sort()") {
        // left with more generic signature for JVM only
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
        doc {
            """
            Sorts the array in-place according to the natural order of its elements.

            $stableSortNote

            @throws ClassCastException if any element of the array is not [Comparable].
            """
        }
        returns("Unit")
        body {
            "if (size > 1) java.util.Arrays.sort(this)"
        }
    }

    val f_sort_range_jvm = fn("sort(fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects)
    } builder {
        doc {
            """
            Sorts a range in the array in-place.
            
            $stableSortNote
            
            ${rangeDoc(hasDefault = true, action = "sort")}
            """
        }
        sample("samples.collections.Arrays.Sorting.sortRangeOfArrayOfComparable")
        returns("Unit")
        body { "java.util.Arrays.sort(this, fromIndex, toIndex)" }
    }

    val f_sort_range = fn("sort(fromIndex: Int = 0, toIndex: Int = size)") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        on(Platform.Common) { since("1.4") }

        typeParam("T : Comparable<T>")
        doc {
            """
            Sorts a range in the array in-place.
            ${if (f == ArraysOfObjects) "\n$stableSortNote\n" else ""}
            ${rangeDoc(hasDefault = true, action = "sort")}
            """
        }
        specialFor(ArraysOfObjects) {
            sample("samples.collections.Arrays.Sorting.sortRangeOfArrayOfComparable")
        }
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) {
            sample("samples.collections.Arrays.Sorting.sortRangeOfArray")
        }
        returns("Unit")

        if (f == ArraysOfUnsigned) {
            since("1.4")
            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                sortArray(this, fromIndex, toIndex)
                """
            }
            return@builder
        }

        on(Platform.JVM) {
            if (f == ArraysOfObjects) since("1.4")
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                "java.util.Arrays.sort(this, fromIndex, toIndex)"
            }
        }
        on(Platform.Native) {
            since("1.4")
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                sortArray(this, fromIndex, toIndex)
                """
            }
        }
        on(Platform.JS) {
            since("1.4")
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                sortArrayWith(this, fromIndex, toIndex, naturalOrder())
                """
            }
            specialFor(ArraysOfPrimitives) {
                body {
                    if (primitive == PrimitiveType.Char && target.backend == Backend.IR || primitive == PrimitiveType.Long) {
                        """
                        AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                        sortArrayWith(this.unsafeCast<Array<T>>(), fromIndex, toIndex, naturalOrder())
                        """
                    } else {
                        """
                        AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                        val subarray = this.asDynamic().subarray(fromIndex, toIndex).unsafeCast<SELF>()
                        subarray.sort()
                        """
                    }
                }
            }
        }
    }

    val f_sortWith_range = fn("sortWith(comparator: Comparator<in T>, fromIndex: Int = 0, toIndex: Int = size)") {
        include(ArraysOfObjects)
    } builder {
        doc {
            """
            Sorts a range in the array in-place with the given [comparator].
            
            $stableSortNote
            
            ${rangeDoc(hasDefault = true, action = "sort")}
            """
        }
        returns("Unit")
        on(Platform.JVM) {
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                "java.util.Arrays.sort(this, fromIndex, toIndex, comparator)"
            }
        }
        on(Platform.Native) {
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                sortArrayWith(this, fromIndex, toIndex, comparator)
                """
            }
        }
        on(Platform.JS) {
            since("1.4")
            suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                sortArrayWith(this, fromIndex, toIndex, comparator)
                """
            }
        }
    }

    val f_sortDescending_range = fn("sortDescending(fromIndex: Int, toIndex: Int)") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        since("1.4")
        doc {
            """
            Sorts elements of the ${f.collection} in the specified range in-place.
            The elements are sorted descending according to their natural sort order.
            ${if (f == ArraysOfObjects) "\n$stableSortNote\n" else ""}
            ${rangeDoc(hasDefault = false, action = "sort")}
            """
        }
        returns("Unit")
        typeParam("T : Comparable<T>")

        specialFor(ArraysOfObjects) {
            body { """sortWith(reverseOrder(), fromIndex, toIndex)""" }
        }
        body(ArraysOfPrimitives, ArraysOfUnsigned) {
            """
            sort(fromIndex, toIndex)
            reverse(fromIndex, toIndex)
            """
        }
    }


    val f_asList = fn("asList()") {
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc { "Returns a [List] that wraps the original array." }
        returns("List<T>")
        on(Platform.JVM) {
            body { """return ArraysUtilJVM.asList(this)""" }
        }
        on(Platform.JS) {
            body { """return ArrayList<T>(this.unsafeCast<Array<Any?>>())""" }
        }

        val objectLiteralImpl = if (primitive in PrimitiveType.floatingPointPrimitives) """
            return object : AbstractList<T>(), RandomAccess {
                override val size: Int get() = this@asList.size
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(element: T): Boolean = this@asList.any { it.toBits() == element.toBits() }
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(element: T): Int = this@asList.indexOfFirst { it.toBits() == element.toBits() }
                override fun lastIndexOf(element: T): Int = this@asList.indexOfLast { it.toBits() == element.toBits() }
            }
            """
        else """
            return object : AbstractList<T>(), RandomAccess {
                override val size: Int get() = this@asList.size
                override fun isEmpty(): Boolean = this@asList.isEmpty()
                override fun contains(element: T): Boolean = this@asList.contains(element)
                override fun get(index: Int): T = this@asList[index]
                override fun indexOf(element: T): Int = this@asList.indexOf(element)
                override fun lastIndexOf(element: T): Int = this@asList.lastIndexOf(element)
            }
            """
        specialFor(ArraysOfPrimitives, ArraysOfUnsigned) {
            on(Platform.JVM) {
                body { objectLiteralImpl }
            }
            on(Platform.JS) {
                if (primitive == PrimitiveType.Char || primitive in PrimitiveType.unsignedPrimitives) {
                    body {
                        """
                        return object : AbstractList<T>(), RandomAccess {
                            override val size: Int get() = this@asList.size
                            override fun isEmpty(): Boolean = this@asList.isEmpty()
                            override fun contains(element: T): Boolean = this@asList.contains(element)
                            override fun get(index: Int): T {
                                AbstractList.checkElementIndex(index, size)
                                return this@asList[index]
                            }
                            override fun indexOf(element: T): Int {
                                @Suppress("USELESS_CAST")
                                if ((element as Any?) !is T) return -1
                                return this@asList.indexOf(element)
                            }
                            override fun lastIndexOf(element: T): Int {
                                @Suppress("USELESS_CAST")
                                if ((element as Any?) !is T) return -1
                                return this@asList.lastIndexOf(element)
                            }
                        }
                        """
                    }
                }
                else {
                    inlineOnly()
                    body { "return this.unsafeCast<Array<T>>().asList()" }
                }
            }
        }
        on(Platform.Native) {
            body { objectLiteralImpl }
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
                    else ->
                        body { "return js(\"[]\").slice.call(this)" }
                }

            }
        }
    }

    fun MemberBuilder.rangeParamDoc(hasDefault: Boolean, action: String): String = """
        @param fromIndex the start of the range (inclusive) to $action${if (hasDefault) ", 0 by default" else ""}.
        @param toIndex the end of the range (exclusive) to $action${if (hasDefault) ", ${f.code.size} of this ${f.collection} by default" else ""}.
        """.trimIndent()

    fun MemberBuilder.rangeThrowsDoc(): String = """
        @throws IndexOutOfBoundsException if [fromIndex] is less than zero or [toIndex] is greater than the ${f.code.size} of this ${f.collection}.
        @throws IllegalArgumentException if [fromIndex] is greater than [toIndex].
        """.trimIndent()

    fun MemberBuilder.rangeDoc(hasDefault: Boolean, action: String): String = """
        ${rangeParamDoc(hasDefault, action)}
        
        ${rangeThrowsDoc()}
        """.trimIndent()

    val f_fill = fn("fill(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        include(InvariantArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
    } builder {
        doc {
            """
            Fills this array or its subrange with the specified [element] value.
            
            ${rangeDoc(hasDefault = true, action = "fill")}
            """
        }
        returns("Unit")

        specialFor(ArraysOfUnsigned) {
            val signedPrimitiveName = primitive!!.name.drop(1)
            body {
                "storage.fill(element.to$signedPrimitiveName(), fromIndex, toIndex)"
            }
        }

        specialFor(InvariantArraysOfObjects, ArraysOfPrimitives) {
            on(Platform.JVM) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                body {
                    "java.util.Arrays.fill(this, fromIndex, toIndex, element)"
                }
            }
            on(Platform.JS) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                since("1.3")
                body {
                    """
                    AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                    nativeFill(element, fromIndex, toIndex)
                    """
                }
                specialFor(ArraysOfPrimitives) {
                    if (primitive == PrimitiveType.Char) {
                        body {
                            """
                            AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                            // We need to call [Char.code] here to eliminate Char-boxing with the new JS function inlining logic
                            nativeFill(element.code, fromIndex, toIndex)
                            """
                        }
                    }
                }
            }
            on(Platform.Native) {
                suppress("ACTUAL_FUNCTION_WITH_DEFAULT_ARGUMENTS")
                since("1.3")
                body {
                    "arrayFill(this, fromIndex, toIndex, element)"
                }
                on(Backend.Wasm) {
                    body {
                        """
                        AbstractList.checkRangeIndexes(fromIndex, toIndex, size)
                        for (index in fromIndex until toIndex) {
                            this[index] = element    
                        }
                        """
                    }
                }
            }
            on(Platform.Common) {
                since("1.3")
            }
        }
    }

    val f_binarySearch = fn("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size)") {
        platforms(Platform.JVM)
        include(ArraysOfObjects, ArraysOfPrimitives, ArraysOfUnsigned)
        exclude(PrimitiveType.Boolean)
    } builder {
        doc {
            """
            Searches the array or the range of the array for the provided [element] using the binary search algorithm.
            The array is expected to be sorted, otherwise the result is undefined.

            If the array contains multiple elements equal to the specified [element], there is no guarantee which one will be found.
            
            @param element the to search for.
            ${rangeParamDoc(hasDefault = true, action = "search in")}

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted.
            
            ${rangeThrowsDoc()}
            """
        }
        returns("Int")
        body {
            "return java.util.Arrays.binarySearch(this, fromIndex, toIndex, element)"
        }

        specialFor(ArraysOfUnsigned) {
            val elementConversion: String
            val midValConversion: String
            val compareFunction: String
            when (primitive!!) {
                PrimitiveType.UByte, PrimitiveType.UShort -> {
                    elementConversion = ".toInt()"
                    midValConversion = ".toInt()"
                    compareFunction = "uintCompare"
                }
                PrimitiveType.UInt -> {
                    elementConversion = ".toInt()"
                    midValConversion = ""
                    compareFunction = "uintCompare"
                }
                PrimitiveType.ULong -> {
                    elementConversion = ".toLong()"
                    midValConversion = ""
                    compareFunction = "ulongCompare"
                }
                else -> error(primitive!!)
            }


            body {
                """
                AbstractList.checkRangeIndexes(fromIndex, toIndex, size)

                val signedElement = element$elementConversion
                var low = fromIndex
                var high = toIndex - 1

                while (low <= high) {
                    val mid = (low + high).ushr(1) // safe from overflows
                    val midVal = storage[mid]
                    val cmp = $compareFunction(midVal$midValConversion, signedElement)

                    if (cmp < 0)
                        low = mid + 1
                    else if (cmp > 0)
                        high = mid - 1
                    else
                        return mid // key found
                }
                return -(low + 1)  // key not found
                """
            }
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

            @param element the element to search for.
            @param comparator the comparator according to which this array is sorted.
            ${rangeParamDoc(hasDefault = true, action = "search in")}

            @return the index of the element, if it is contained in the array within the specified range;
            otherwise, the inverted insertion point `(-insertion point - 1)`.
            The insertion point is defined as the index at which the element should be inserted,
            so that the array (or the specified subrange of array) still remains sorted according to the specified [comparator].

            ${rangeThrowsDoc()}
            """
        }
        returns("Int")
        body {
            "return java.util.Arrays.binarySearch(this, fromIndex, toIndex, element, comparator)"
        }
    }
}
