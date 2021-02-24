// IGNORE_BACKEND: JS

//KT-42357

// FILE: main.kt
external class FieldPath {
    constructor(
        arg: Int = definedExternally,
        vararg args: String
    )

    constructor(
        arg: Int = definedExternally,
        vararg args: String,
        o: Long
    )
}

external val ctorCallArgs: Array<String>

fun box(): String {
    FieldPath()
    if (ctorCallArgs.size != 0) return "fail: $ctorCallArgs arguments"

    FieldPath(1)
    if (ctorCallArgs.size != 1 || js("typeof ctorCallArgs[0] !== 'number'")) return "fail1: $ctorCallArgs arguments"

    FieldPath(2, "p0", "p1", "p3")
    if (ctorCallArgs.size != 4 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail2: $ctorCallArgs arguments"

    FieldPath(3, args = arrayOf("p0", "p1"))
    if (ctorCallArgs.size != 3 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail3: $ctorCallArgs arguments"

    FieldPath(4, *arrayOf("p0", "p1"))
    if (ctorCallArgs.size != 3 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail4: $ctorCallArgs arguments"

    FieldPath(5, args = *arrayOf("p0", "p1"))
    if (ctorCallArgs.size != 3 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail5: $ctorCallArgs arguments"

    FieldPath(42, "a", "b", "c", o = 99L)
    if (ctorCallArgs.size != 5 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail6: $ctorCallArgs arguments"

    FieldPath(5, args = *arrayOf("p0", "p1"), o = 87L)
    if (ctorCallArgs.size != 4 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail6: $ctorCallArgs arguments"

    FieldPath(4, *arrayOf("p0", "p1"), o = 99L)
    if (ctorCallArgs.size != 4 || js("typeof ctorCallArgs[0] !== 'number'") || js("typeof ctorCallArgs[1] !== 'string'"))
        return "fail7: $ctorCallArgs arguments"

    FieldPath(11, *arrayOf(), o = 123456L)
    if (ctorCallArgs.size != 2 || js("typeof ctorCallArgs[0] !== 'number'"))
        return "fail9: $ctorCallArgs arguments"

    return "OK"
}

// FILE: main.js
var ctorCallArgs;
function FieldPath() {
    ctorCallArgs = arguments;
}