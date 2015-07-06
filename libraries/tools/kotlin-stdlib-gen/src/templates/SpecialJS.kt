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

    templates add f("copyOfRange(from: Int, to: Int)") {
        // TODO: Arguments checking as in java?
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array." }
        inline(true)
        annotations("""suppress("NOTHING_TO_INLINE")""")
        returns("SELF")
        returns(ArraysOfObjects) { "Array<T>" }
        body {
            "return (this: dynamic).slice(from, to)"
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
        doc { "Returns an array containing all elements of the original array and then the given [element]." }
        body(ArraysOfObjects, ArraysOfPrimitives) {
            """
            val result = this.copyOf()
            (result: dynamic).push(element)
            return result as SELF
            """
        }
    }

    templates add f("plus(collection: Collection<T>)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        returns("SELF")
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
        body {
            """
            return (this: dynamic).concat(array)
            """
        }
    }



    return templates
}