// NON_EXISTENT_CLASS
// NO_VALIDATION

@Suppress("UNRESOLVED_REFERENCE")
object NonExistentType {
    val a: ABCDEF? = null
    val b: List<ABCDEF>? = null
    val c: (ABCDEF) -> Unit = { f -> }
    val d: ABCDEF<String, (List<ABCDEF>) -> Unit>? = null

    fun a(a: ABCDEF, s: String): ABCDEF {}
    fun b(s: String): ABCDEF {}
}