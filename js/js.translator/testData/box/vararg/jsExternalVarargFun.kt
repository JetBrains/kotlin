//KT-42357

// FILE: main.kt
external fun create(
    arg: Int = definedExternally,
    vararg args: String
) : Array<String>

external fun create(
    arg: Int = definedExternally,
    vararg args: String,
    o: Long
) : Array<String>

fun box(): String {
    val zeroArgs = create()
    if (zeroArgs.size != 0) return "fail: $zeroArgs arguments"

    val oneArg = create(1)
    if (oneArg.size != 1 || js("typeof oneArg[0] !== 'number'"))
        return "fail1: $oneArg arguments"

    val varArgs = create(2, "p0", "p1", "p3")
    if (varArgs.size != 4 || js("typeof varArgs[0] !== 'number'") || js("typeof varArgs[1] !== 'string'"))
        return "fail2: $varArgs arguments"

    val namedParameter = create(3, args = arrayOf("p0", "p1"))
    if (namedParameter.size != 3 || js("typeof namedParameter[0] !== 'number'") || js("typeof namedParameter[1] !== 'string'"))
        return "fail3: $namedParameter arguments"

    val spreadArgs = create(4, *arrayOf("p0", "p1"))
    if (spreadArgs.size != 3 || js("typeof spreadArgs[0] !== 'number'") || js("typeof spreadArgs[1] !== 'string'"))
        return "fail4: $spreadArgs arguments"

    val spreadNamedArgs = create(5, args = *arrayOf("p0", "p1"))
    if (spreadNamedArgs.size != 3 || js("typeof spreadNamedArgs[0] !== 'number'") || js("typeof spreadNamedArgs[1] !== 'string'"))
        return "fail5: $spreadNamedArgs arguments"

    val argAfterVararg = create(42, "a", "b", "c", o = 99L)
    if (argAfterVararg.size != 5 || js("typeof argAfterVararg[0] !== 'number'") || js("typeof argAfterVararg[1] !== 'string'"))
        return "fail6: $argAfterVararg arguments"

    val argAfterSpreadNamedVararg = create(5, args = *arrayOf("p0", "p1"), o = 87L)
    if (argAfterSpreadNamedVararg.size != 4 || js("typeof argAfterSpreadNamedVararg[0] !== 'number'") || js("typeof argAfterSpreadNamedVararg[1] !== 'string'"))
        return "fail7: $argAfterSpreadNamedVararg arguments"

    val argAfterSpreadVararg = create(4, *arrayOf("p0", "p1"), o = 99L)
    if (argAfterSpreadVararg.size != 4 || js("typeof argAfterSpreadVararg[0] !== 'number'") || js("typeof argAfterSpreadVararg[1] !== 'string'"))
        return "fail8: $argAfterSpreadVararg arguments"

    val argAfterSpreadEmptyVararg = create(11, *arrayOf(), o = 123456L)
    if (argAfterSpreadEmptyVararg.size != 2 || js("typeof argAfterSpreadEmptyVararg[0] !== 'number'"))
        return "fail9: $argAfterSpreadEmptyVararg arguments"

    return "OK"
}

// FILE: main.js
function create() {
    return arguments
}
