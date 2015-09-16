// !specifyLocalVariableTypeByDefault: true
fun foo(b: Boolean) {
    var s: String? = null
    if (b) {
        s = "abc"
    }
}