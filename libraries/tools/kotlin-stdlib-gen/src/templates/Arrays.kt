package templates

import templates.Family.*

fun arrays(): List<GenericFunction> {
    val templates = iterables()

    templates add f("isEmpty()") {
        absentFor(Arrays)
        isInline = false
        doc = "Returns true if the array is empty"
        returns("Boolean")
        body {
            "return size == 0"
        }
    }

    templates add f("isNotEmpty()") {
        absentFor(Arrays)
        isInline = false
        doc = "Returns true if the array is empty"
        returns("Boolean")
        body {
            "return !isEmpty()"
        }
    }

    templates add f("indexOf(item: T)") {
        absentFor(PrimitiveArrays)
        isInline = false
        doc = "Returns first index of item, or -1 if the array does not contain item"
        returns("Int")
        body {
            """
                if (item == null) {
                    for (i in indices) {
                        if (this[i] == null) {
                            return i
                        }
                    }
                } else {
                    for (i in indices) {
                        if (item == this[i]) {
                            return i
                        }
                    }
                }
                return -1
           """
        }
    }

    // implementation for PrimitiveArrays is separate from Arrays, because they cannot hold null elements
    templates add f("indexOf(item: T)") {
        absentFor(Arrays)
        isInline = false
        doc = "Returns first index of item, or -1 if the array does not contain item"
        returns("Int")
        body {
            """
                for (i in indices) {
                    if (item == this[i]) {
                        return i
                    }
                }
                return -1
           """
        }
    }

    templates add f("contains(item: T)") {
        isInline = false
        doc = "Returns true if the array contains the item"
        returns("Boolean")
        body {
            """
                return this.indexOf(item) >= 0
           """
        }
    }

    return templates.sort()
}