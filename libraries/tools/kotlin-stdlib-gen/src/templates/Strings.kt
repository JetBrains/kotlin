package templates

import templates.Family.*

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
        typeParam("A : Appendable")
        returns { "A" }
        body {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    if (transform != null)
                        buffer.append(transform(element))
                    else
                        buffer.append(if (element == null) "null" else element.toString())
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
        exclude(Strings)
        body(ArraysOfPrimitives) {
            """
            buffer.append(prefix)
            var count = 0
            for (element in this) {
                if (++count > 1) buffer.append(separator)
                if (limit < 0 || count <= limit) {
                    if (transform != null)
                        buffer.append(transform(element))
                    else
                        buffer.append(element.toString())
                } else break
            }
            if (limit >= 0 && count > limit) buffer.append(truncated)
            buffer.append(postfix)
            return buffer
            """
        }
    }

    templates add f("joinTo(buffer: A, separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\", transform: ((T) -> String)? = null)") {
        deprecate { forBinaryCompatibility }
        typeParam("A : Appendable")
        returns { "A" }
        body {
            """
            return joinTo(buffer, separator, prefix, postfix, limit, truncated, transform)
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

        exclude(Strings)
        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
            """
        }
    }

    templates add f("joinToString(separator: String = \", \", prefix: String = \"\", postfix: String = \"\", limit: Int = -1, truncated: String = \"...\", transform: ((T) -> String)? = null)") {
        deprecate { forBinaryCompatibility }
        returns("String")
        body {
            """
            return joinTo(StringBuilder(), separator, prefix, postfix, limit, truncated, transform).toString()
            """
        }
    }

    return templates
}