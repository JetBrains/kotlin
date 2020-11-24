// EXPECTED_REACHABLE_NODES: 1239
// FILE: main.kt
external class TailArgs(
    p0: String = definedExternally,
    p1: String = definedExternally,
    p2: String = definedExternally,
    p3: String = definedExternally,
    p4: String = definedExternally,
)

external val ctorArgs: Array<String>

fun box(): String {
    val p2 = "p2"

    TailArgs()
    if (ctorArgs.size != 0) return "fail2: $ctorArgs arguments instead 0"

    TailArgs(p2 = p2)
    if (ctorArgs.size != 3 || ctorArgs[2] != p2) return "fail3: $ctorArgs arguments instead 3"

    return "OK"
}

// FILE: main.js
var ctorArgs;
function TailArgs() {
    ctorArgs = arguments
}
