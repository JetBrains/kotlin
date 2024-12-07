// KIND: STANDALONE
// MODULE: main
// FILE: main.kt

enum class Enum(var i: Int, private val s: String) {
    a(1, "str"), b(5, "rts");

    fun print(): String = "$i - $s"
}

fun enumId(e: kotlin.Enum<*>): kotlin.Enum<*> = e
