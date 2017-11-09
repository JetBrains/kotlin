package templates

import templates.Family.*
import templates.SequenceClass.*

fun strings(): List<GenericFunction> {
    val templates = arrayListOf<GenericFunction>()

    templates add f("joinTo(buffer: A, separator: CharSequence = \", \", prefix: CharSequence = \"\", postfix: CharSequence = \"\", limit: Int = -1, truncated: CharSequence = \"...\", transform: ((T) -> CharSequence)? = null)") {
        doc {
            """
            Appends the string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.

            If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
            elements will be appended, followed by the [truncated] string (which defaults to "...").
            """
        }
        sequenceClassification(terminal)
        typeParam("A : Appendable")
        returns { "A" }
        body {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    buffer.appendElement(element, transform)
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
        exclude(Strings)
        bodyForTypes(ArraysOfPrimitives, *defaultPrimitives.toTypedArray()) { primitive ->
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    if (transform != null)
                        buffer.append(transform(element))
                    else
                        buffer.append(${if (primitive == PrimitiveType.Char) "element" else "element.toString()"})
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
    }

    templates add f("joinToString(separator: CharSequence = \", \", prefix: CharSequence = \"\", postfix: CharSequence = \"\", limit: Int = -1, truncated: CharSequence = \"...\", transform: ((T) -> CharSequence)? = null)") {
        doc {
            """
            Creates a string from all the elements separated using [separator] and using the given [prefix] and [postfix] if supplied.

            If the collection could be huge, you can specify a non-negative value of [limit], in which case only the first [limit]
            elements will be appended, followed by the [truncated] string (which defaults to "...").
            """
        }
        sequenceClassification(terminal)

        exclude(Strings)
        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
            """
        }
    }

    return templates
}