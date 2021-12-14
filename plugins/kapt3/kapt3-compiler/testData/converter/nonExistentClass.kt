// CORRECT_ERROR_TYPES
// NON_EXISTENT_CLASS
// NO_VALIDATION

@Suppress("UNRESOLVED_REFERENCE")
object NonExistentType {
    val a: ABCDEF? = null
    val b: List<ABCDEF>? = null
    val c: (ABCDEF) -> Unit = { f: ABCDEF -> }
    val d: ABCDEF<String, (List<ABCDEF>) -> Unit>? = null
    
    val foo: Foo get() = Foo()

    fun a(a: ABCDEF, s: String): ABCDEF {}
    fun b(s: String): ABCDEF {}
}
