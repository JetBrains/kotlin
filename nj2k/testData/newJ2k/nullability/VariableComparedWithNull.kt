// !specifyLocalVariableTypeByDefault: true
fun bar(): String? {
    return null
}
fun foo() {
    val s: String? = bar()
    if (s != null) {
        zoo(s)
    }
}