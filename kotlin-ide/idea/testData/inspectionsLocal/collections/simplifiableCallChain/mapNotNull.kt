// WITH_RUNTIME

val x = listOf(1, 0, 2).<caret>map { if (it != 0) it else null }.filterNotNull()