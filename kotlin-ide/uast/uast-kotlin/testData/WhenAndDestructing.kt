fun getElementsAdditionalResolve(string: String): String {

    val arr = listOf("1", "2")

    when (string) {
        "aaaa" -> {
            return "bindingContext"
        }

        else -> {
            val (bindingContext, statementFilter) = arr
            return bindingContext
        }
    }
}


