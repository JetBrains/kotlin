// !specifyLocalVariableTypeByDefault: true
fun foo() {
    val s: String? = bar()
    if (s != null) {
        zoo(s)
    }
}