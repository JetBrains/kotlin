// TARGET_BACKEND: WASM
// EXPECTED_REACHABLE_NODES: 1280

// NOTE: Hash codes are the same as in Native and JVM

fun box(): String {
    var value = (3).hashCode()
    if (value != 3) return "fail1"

    value = (3.14).hashCode()
    if (value != 300063655) return "fail2"

    value = (3.14159).hashCode()
    if (value != -1340954729) return "fail3"

    value = (1e80).hashCode()
    if (value != 24774576) return "fail4"

    value = (1e81).hashCode()
    if (value != -1007271154) return "fail5"

    return "OK"
}
