// !specifyLocalVariableTypeByDefault: true
internal fun foo() {
    val s: String? = bar()
    if (s != null) {
        zoo(s)
    }
}