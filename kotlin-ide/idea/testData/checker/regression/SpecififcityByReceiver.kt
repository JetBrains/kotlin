// FIR_COMPARISON

fun Any.<warning descr="[EXTENSION_SHADOWED_BY_MEMBER] Extension is shadowed by a member: public open operator fun equals(other: Any?): Boolean">equals</warning>(<warning descr="[UNUSED_PARAMETER] Parameter 'other' is never used">other</warning> : Any?) : Boolean = true

fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {

    val command : Any = 1

    command<warning descr="[UNNECESSARY_SAFE_CALL] Unnecessary safe call on a non-null receiver of type Any">?.</warning>equals(null)
    command.equals(null)
}