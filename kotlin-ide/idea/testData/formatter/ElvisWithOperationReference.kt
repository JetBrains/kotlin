infix fun String.concat(other: String): String = this
fun binaryContext(anchor: String?) {
    val v1 = "1234567890... add many chars ...1234567890" concat "b"
    val v2 = anchor ?: "1234567890... add many chars ...1234567890" concat "b"
    val v3 = anchor ?: "1234567890... add many chars ...1234567890" concat "1234567890... add many chars ...1234567890" concat "1234567890... add many chars ...1234567890" concat "b"
}

// SET_INT: WRAP_ELVIS_EXPRESSIONS = 2
