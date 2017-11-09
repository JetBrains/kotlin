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
            annotation("""@Suppress("UNUSED_PARAMETER")""")
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
            annotation("""@Suppress("UNUSED_PARAMETER")""")
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
}
