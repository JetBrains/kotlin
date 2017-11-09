package templates

import templates.Family.*

fun arrays(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("isEmpty()") {
        inline(Inline.Only)
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns `true` if the array is empty." }
        returns("Boolean")
        body {
            "return size == 0"
        }
    }

    templates add f("isNotEmpty()") {
        inline(Inline.Only)
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns `true` if the array is not empty." }
        returns("Boolean")
        body {
            "return !isEmpty()"
        }
    }


    templates add pval("lastIndex") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns the last valid index for the array." }
        returns("Int")
        body {
            "get() = size - 1"
        }
    }

    templates add pval("indices") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns the range of valid indices for the array." }
        returns("IntRange")
        body {
            "get() = IntRange(0, lastIndex)"
        }
    }

    templates add f("contentEquals(other: SELF)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
        infix(true)
        doc {
            """
            Returns `true` if the two specified arrays are *structurally* equal to one another,
            i.e. contain the same number of the same elements in the same order.
            """
        }
        returns("Boolean")
        body(Platform.JVM) { "return java.util.Arrays.equals(this, other)" }

        annotations(Platform.JS, """
            @library("arrayEquals")
            @Suppress("UNUSED_PARAMETER")
        """)
        body(Platform.JS) { "definedExternally" }

    }

    templates add f("contentDeepEquals(other: SELF)") {
        only(ArraysOfObjects)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
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
        body(Platform.JVM) { "return java.util.Arrays.deepEquals(this, other)" }
        annotations(Platform.JS, """
            @library("arrayDeepEquals")
            @Suppress("UNUSED_PARAMETER")
        """)
        body(Platform.JS) { "definedExternally" }
    }

    templates add f("contentToString()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
        doc {
            """
            Returns a string representation of the contents of the specified array as if it is [List].

            @sample samples.collections.Arrays.ContentOperations.contentToString
            """
        }
        returns("String")
        body(Platform.JVM) { "return java.util.Arrays.toString(this)" }
        annotations(Platform.JS, """@library("arrayToString")""")
        body(Platform.JS) { "definedExternally" }
    }

    templates add f("contentDeepToString()") {
        only(ArraysOfObjects)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
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
        body(Platform.JVM) { "return java.util.Arrays.deepToString(this)" }
        annotations(Platform.JS, """@library("arrayDeepToString")""")
        body(Platform.JS) { "definedExternally" }
    }

    templates add f("contentHashCode()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
        doc {
            "Returns a hash code based on the contents of this array as if it is [List]."
        }
        returns("Int")
        body(Platform.JVM) { "return java.util.Arrays.hashCode(this)" }
        annotations(Platform.JS, """@library("arrayHashCode")""")
        body(Platform.JS) { "definedExternally" }
    }

    templates add f("contentDeepHashCode()") {
        only(ArraysOfObjects)
        since("1.1")
        inline(Platform.JVM, Inline.Only)
        doc {
            """
            Returns a hash code based on the contents of this array as if it is [List].
            Nested arrays are treated as lists too.

            If any of arrays contains itself on any nesting level the behavior is undefined.
            """
        }
        returns("Int")
        body(Platform.JVM) { "return java.util.Arrays.deepHashCode(this)" }
        annotations(Platform.JS, """@library("arrayDeepHashCode")""")
        body(Platform.JS) { "definedExternally" }
    }

    templates addAll PrimitiveType.defaultPrimitives.map { primitive ->
        val arrayType = primitive.name + "Array"
        f("to$arrayType()") {
            only(ArraysOfObjects, Collections)
            buildFamilies.default!!.forEach { family -> onlyPrimitives(family, primitive) }
            doc(ArraysOfObjects) { "Returns an array of ${primitive.name} containing all of the elements of this generic array." }
            doc(Collections) { "Returns an array of ${primitive.name} containing all of the elements of this collection." }
            returns(arrayType)
            // TODO: Use different implementations for JS
            body {
                """
                val result = $arrayType(size)
                for (index in indices)
                    result[index] = this[index]
                return result
                """
            }
            body(Collections) {
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

    return templates
}