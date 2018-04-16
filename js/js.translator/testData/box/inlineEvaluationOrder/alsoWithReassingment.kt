// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1111
private var _value: String = "OK"

private inline fun String.myAlso(f: (String) -> Unit): String {
    f(this)
    return this
}

fun overrideValueAndReturnOld(newValue: String) = _value.myAlso { _value = newValue }

fun box() = overrideValueAndReturnOld("fail")