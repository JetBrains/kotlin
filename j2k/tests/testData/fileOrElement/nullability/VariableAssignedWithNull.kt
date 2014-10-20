// !specifyLocalVariableTypeByDefault: true
fun foo(b: Boolean) {
    var s: String? = "abc"
    if (b) {
        s = null
    }
}