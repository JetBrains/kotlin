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
            ArraysOfObjects, ArraysOfPrimitives, InvariantArraysOfObjects -> "array"
            Strings, Sequences, Maps, Lists, Sets, Ranges -> name.singularize().decapitalize()
            else -> "collection"
        }

    val Family.mapResult: String
        get() = when (this) {
            Sequences -> "sequence"
            else -> "list"
        }

    private fun String.singularize() = removeSuffix("s")

    fun String.prefixWithArticle() = (if ("aeiou".any { this.startsWith(it, ignoreCase = true) }) "an " else "a ").concat(this)

}
