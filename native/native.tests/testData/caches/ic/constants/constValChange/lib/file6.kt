package test

fun constInWhen(x: Int): String = when (x) {
    VALUE -> "value"
    else -> "other"
}
