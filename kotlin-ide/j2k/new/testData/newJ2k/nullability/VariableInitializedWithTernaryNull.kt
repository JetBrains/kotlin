// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
fun foo(b: Boolean) {
    val s: String? = if (b) "abc" else null
}