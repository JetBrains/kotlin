inline fun <reified T> marker(value: Any): String = if (value is T) "A0" else "BAD"
