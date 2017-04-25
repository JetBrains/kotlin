// EXPECTED_REACHABLE_NODES: 487

fun box(): String {


    var value = (3).hashCode()
    if (value != 3) return "fail1: $value"

    value = (3.14).hashCode()
    if (value != 319176039) return "fail2: $value"

    value = (3.14159).hashCode()
    if (value != -1321819243) return "fail3: $value"

    value = (1e80).hashCode()
    if (value != 314940496) return "fail4: $value"

    value = (1e81).hashCode()
    if (value != 1519485350) return "fail5: $value"

    return "OK"
}
