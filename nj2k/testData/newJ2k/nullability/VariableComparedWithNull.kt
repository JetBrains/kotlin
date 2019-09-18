// !specifyLocalVariableTypeByDefault: true

fun bar(): String? {
    return null
}

fun foo() {
    val s: String? = bar()
    s?.let { zoo(it) }
}