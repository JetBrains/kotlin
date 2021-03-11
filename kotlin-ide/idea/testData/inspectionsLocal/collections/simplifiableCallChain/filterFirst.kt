// WITH_RUNTIME

val nullableList: List<String>? = listOf("1", "")
val x = nullableList?.<caret>filter { it.isNotEmpty() }?.first()