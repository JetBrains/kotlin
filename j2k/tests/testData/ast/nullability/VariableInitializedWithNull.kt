// !specifyLocalVariableTypeByDefault: true
fun foo(b: Boolean) {
    val s: String? = null
    if (b) {
        s = "abc"
    }
}