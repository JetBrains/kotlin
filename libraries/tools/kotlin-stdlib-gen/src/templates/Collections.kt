package templates

import java.util.ArrayList
import templates.Family.*

fun collections(): List<GenericFunction> {

    val templates = ArrayList<GenericFunction>()

    templates add f("requireNoNulls()") {
        absentFor(PrimitiveArrays) // Those are inherently non-nulls
        doc = "Returns a original Iterable containing all the non-*null* elements, throwing an [[IllegalArgumentException]] if there are any null elements"
        typeParam("T:Any")
        toNullableT = true
        returns("SELF")

        body {
            val THIS = "\$this"
            """
                for (element in this) {
                    if (element == null) {
                        throw IllegalArgumentException("null element found in $THIS")
                    }
                }
                return this as SELF
            """
        }

    }

    return templates.sort()
}