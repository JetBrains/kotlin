// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
fun bar(): String? {
    return null
}

fun foo() {
    val s: String? = bar()
    s?.let { zoo(it) }
}