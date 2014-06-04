// !specifyLocalVariableTypeByDefault: true
fun foo(b: Boolean) {
    val s: String? = "abc"
    if (b) {
        s = null
    }
}