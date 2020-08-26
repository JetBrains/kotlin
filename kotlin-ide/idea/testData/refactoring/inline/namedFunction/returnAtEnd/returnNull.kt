fun <caret>a(): Int? {
    return null
}

fun test() {
    val result: Int? = a()?.dec()
}