// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1281
fun box(): String {
    val a = CharArray(1)
    val aType = jsTypeOf(a.asDynamic()[0])
    if (aType != "number") return "fail1: $aType"

    a[0] = 'Q'
    val aType2 = jsTypeOf(a.asDynamic()[0])
    if (aType2 != "number") return "fail2: $aType2"

    val aType3 = jsTypeOf(a[0].asDynamic())
    if (aType3 != "number") return "fail3: $aType3"

    val b = Array<Char>(1) { 'Q' }
    val bType = jsTypeOf(b.asDynamic()[0])
    if (bType != "object") return "fail4: $bType"

    b[0] = 'W'
    val bType2 = jsTypeOf(b.asDynamic()[0])
    if (bType2 != "object") return "fail5: $bType2"

    val bType3 = jsTypeOf(b[0].asDynamic())
    if (bType3 != "number") return "fail6: $bType3"

    return "OK"
}