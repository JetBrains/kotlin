// WITH_RUNTIME

val x = listOf(1, 2, 3).<caret>map {
    val sb = StringBuilder()
    sb.append(it).append(" + ").append(it)
    sb
}.joinToString(prefix = "= ", separator = " + ")