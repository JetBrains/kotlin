// !IGNORE_FIR

fun getElementsAdditionalResolve(string: String): String {

    val arr = listOf("1", "2")

    when (string) {
        "aaaa" -> {
            return "bindingContext"
        }

        "empty-switch" -> {}

        else -> {
            val (bindingContext, statementFilter) = arr
            return bindingContext
        }
    }
}


