package templates

import templates.Family.*

object DocExtensions {

    val Family.element: String
        get() = when (this) {
            Strings, CharSequences -> "character"
            Maps -> "entry"
            else -> "element"
        }

    val Family.collection: String
        get() = when (this) {
            CharSequences -> "char sequence"
            Strings -> "string"
            Sequences -> "sequence"
            Maps -> "map"
            ArraysOfObjects, ArraysOfPrimitives, InvariantArraysOfObjects -> "array"
            else -> "collection"
        }

    val Family.mapResult: String
        get() = when (this) {
            Sequences -> "sequence"
            else -> "list"
        }

    fun String.prefixWithArticle() = (if ("aeiou".any { this.startsWith(it, ignoreCase = true) }) "an " else "a ").concat(this)

}
