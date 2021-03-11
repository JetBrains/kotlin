// WITH_RUNTIME

val x = listOf(1, 2, 3).<caret>map { "$it*$it" }.joinToString(prefix = "= ", separator = " + ")