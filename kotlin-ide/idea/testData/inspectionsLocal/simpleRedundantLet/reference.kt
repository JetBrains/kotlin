// WITH_RUNTIME

val a = 42.let<caret> { t -> t }.also { println(it) }