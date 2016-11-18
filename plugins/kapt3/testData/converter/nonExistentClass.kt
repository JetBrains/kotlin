// NON_EXISTENT_CLASS

@Suppress("UNRESOLVED_REFERENCE")
object NonExistentType {
    val a: ABCDEF? = null
    val b: List<ABCDEF>? = null
    val c: (ABCDEF) -> Unit = { f -> }

    fun a(a: ABCDEF, s: String): ABCDEF {}
    fun b(s: String): ABCDEF {}
}