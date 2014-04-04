package templates

import templates.Family.*

fun specialJVM(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("copyOfRange(from: Int, to: Int)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of range of original array" }
        returns("SELF")
        body {
            "return Arrays.copyOfRange(this, from, to)"
        }
    }

    templates add f("copyOf()") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array" }
        returns("SELF")
        body {
            "return Arrays.copyOf(this, size)"
        }
        body(ArraysOfObjects) {
            "return Arrays.copyOf(this, size) as Array<T>"
        }
    }

    // This overload can cause nulls if array size is expanding, hence different return overload
    templates add f("copyOf(newSize: Int)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Returns new array which is a copy of the original array" }
        returns("SELF")
        body {
            "return Arrays.copyOf(this, newSize)"
        }
        returns(ArraysOfObjects) { "Array<T?>" }
        body(ArraysOfObjects) {
            "return Arrays.copyOf(this, newSize) as Array<T?>"
        }
    }

    templates add f("fill(element: T)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        doc { "Fills original array with the provided value" }
        returns { "SELF" }
        returns(ArraysOfObjects) { "Array<out T>" }
        body {
            """
            Arrays.fill(this, element)
            return this
            """
        }
    }

    templates add f("binarySearch(element: T, fromIndex: Int = 0, toIndex: Int = size - 1)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Searches array or range of array for provided element index using binary search algorithm. Array is expected to be sorted." }
        returns("Int")
        body {
            "return Arrays.binarySearch(this, fromIndex, toIndex, element)"
        }
    }

    templates add f("sort(fromIndex : Int = 0, toIndex : Int = size - 1)") {
        only(ArraysOfObjects, ArraysOfPrimitives)
        exclude(PrimitiveType.Boolean)
        doc { "Sorts array or range in array inplace" }
        returns("Unit")
        body {
            "Arrays.sort(this, fromIndex, toIndex)"
        }
    }

    templates add f("filterIsInstanceTo(collection: C, klass: Class<R>)") {
        doc { "Appends all elements that are instances of specified class into the given *collection*" }
        typeParam("C: MutableCollection<in R>")
        typeParam("R: T")
        returns("C")
        exclude(ArraysOfPrimitives)
        body {
            """
            for (element in this) if (klass.isInstance(element)) collection.add(element as R)
            return collection
            """
        }
    }

    templates add f("filterIsInstance(klass: Class<R>)") {
        doc { "Returns a list containing all elements that are instances of specified class" }
        typeParam("R: T")
        returns("List<R>")
        body {
            """
            return filterIsInstanceTo(ArrayList<R>(), klass)
            """
        }
        exclude(ArraysOfPrimitives)

        doc(Streams) { "Returns a stream containing all elements that are instances of specified class" }
        returns(Streams) { "Stream<T>" }
        body(Streams) {
            """
            return FilteringStream(this, true, { klass.isInstance(it) })
            """
        }
    }

    return templates
}