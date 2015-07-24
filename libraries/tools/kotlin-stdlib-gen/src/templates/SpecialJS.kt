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
            val al = ArrayList<T>()
            (al: dynamic).array = this    // black dynamic magic
            return al
            """
        }

        inline(true, ArraysOfPrimitives)
        body(ArraysOfPrimitives) {"""return (this as Array<T>).asList()"""}
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
            return copyOf() as Array<T>
            """
        }
    }

    templates add f("copyOfRange(fromIndex: Int, toIndex: Int)") {
        // TODO: Arguments checking as in java?
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        inline(true)
        annotations("""suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            "return (this: dynamic).slice(fromIndex, toIndex)"
        }
    }

    templates add f("copyOf()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array." }
        inline(true)
        annotations("""suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            "return (this: dynamic).slice(0)"
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


    templates add f("plus(element: T)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        inline(true)
        annotations("""suppress("NOTHING_TO_INLINE")""")
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body() {
            """
            return (this: dynamic).concat(arrayOf(element))
            """
        }
    }

    templates add f("plus(collection: Collection<T>)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        doc { "Returns an array containing all elements of the original array and then all elements of the given [collection]." }
        body {
            """
            return arrayPlusCollection(this, collection)
            """
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("plus(array: SELF)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns an array containing all elements of the original array and then all elements of the given [array]." }
        inline(true)
        annotations("""suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            """
            return (this: dynamic).concat(array)
            """
        }
    }

    templates add f("sort(comparison: (T, T) -> Int)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        annotations("native")
        returns("Unit")
        doc { "Sorts the array inplace according to the order specified by the given [comparison] function." }
        body { "return noImpl" }
    }

    templates add f("sort()") {
        only(ArraysOfPrimitives)
        only(numericPrimitives + PrimitiveType.Char)
        exclude(PrimitiveType.Long)
        returns("Unit")
        doc { "Sorts the array inplace." }
        annotations("""library("primitiveArraySort")""")
        body { "return noImpl" }
    }

    templates add f("sort()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        only(PrimitiveType.Long)
        typeParam("T: Comparable<T>")
        returns("Unit")
        doc { "Sorts the array inplace." }
        body {
            """
            sort { a: T, b: T -> a.compareTo(b) }
            """
        }
    }


    return templates
}