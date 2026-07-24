inline fun <reified T> marker(value: Any): String = if (value is T) "A1" else "BAD"
