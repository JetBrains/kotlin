// !specifyLocalVariableTypeByDefault: true
internal fun foo(b: Boolean) {
    val s: String? = (if (b) "abc" else null)
}