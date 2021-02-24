// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
fun foo(b: Boolean) {
    var s: String? = "abc"
    if (b) {
        s = null
    }
}