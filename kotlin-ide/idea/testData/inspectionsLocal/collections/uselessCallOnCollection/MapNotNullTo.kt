// WITH_RUNTIME

val x = listOf("1").<caret>mapNotNullTo(mutableSetOf()) { it.toInt() }