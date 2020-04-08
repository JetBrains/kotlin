// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
fun foo() {
    val s: String? = bar()
    if (s != null) {
        zoo(s)
    }
}