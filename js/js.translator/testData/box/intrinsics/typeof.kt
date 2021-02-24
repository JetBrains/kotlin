// EXPECTED_REACHABLE_NODES: 1303
external val definedVariableX: Int
external val undefinedVariableX: Int

fun box(): String {
    if (jsTypeOf(definedVariableX) != "number") return "Fail 1"
    if (jsTypeOf(js("definedVariableX")) != "number") return "Fail 2"
    if (jsTypeOf(undefinedVariableX) != "undefined") return "Fail 3"
    if (jsTypeOf(js("undefinedVariableX")) != "undefined") return "Fail 4"

    return "OK"
}