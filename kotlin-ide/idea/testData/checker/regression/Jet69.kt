// FIR_COMPARISON

class Command() {}

fun parse(<warning descr="[UNUSED_PARAMETER] Parameter 'cmd' is never used">cmd</warning>: String): Command? { return null  }

fun Any.<warning descr="[EXTENSION_SHADOWED_BY_MEMBER] Extension is shadowed by a member: public open operator fun equals(other: Any?): Boolean">equals</warning>(other : Any?) : Boolean = this === other

fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {
    val command = parse("")
    if (command == null) <warning descr="[UNUSED_EXPRESSION] The expression is unused">1</warning> // error on this line, but must be OK
}
