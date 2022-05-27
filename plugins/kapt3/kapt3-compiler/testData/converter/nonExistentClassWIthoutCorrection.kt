// NON_EXISTENT_CLASS
// NO_VALIDATION

@file:Suppress("UNRESOLVED_REFERENCE")

typealias String2 = String
typealias Coocoo = ABC
typealias Coocoo2<T> = ABC<T>
typealias Coocoo3<X> = ABC<String, X>

object NonExistentType {
    val a: ABCDEF? = null
    val b: List<ABCDEF>? = null
    val c: (ABCDEF) -> Unit = { f: ABCDEF -> }
    val d: ABCDEF<String, (List<ABCDEF>) -> Unit>? = null

    lateinit var string2: String2

    lateinit var coocoo: Coocoo
    lateinit var coocoo2: Coocoo2<String>
    lateinit var coocoo21: Coocoo2<Coocoo>
    lateinit var coocoo3: Coocoo3<String>
    lateinit var coocoo31: Coocoo3<Coocoo2<Coocoo>>

    fun a(a: ABCDEF, s: String): ABCDEF {}
    fun b(s: String): ABCDEF {}
}