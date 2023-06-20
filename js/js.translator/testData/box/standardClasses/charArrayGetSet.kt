// EXPECTED_REACHABLE_NODES: 1281
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

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

    if (a.asDynamic().constructor.name != "Uint16Array") return "fail7: ${a.asDynamic().constructor.name}"
    if (b.asDynamic().constructor.name != "Array") return "fail8: ${b.asDynamic().constructor.name}"

    val c = charArrayOf('Q')
    val cType = jsTypeOf(c.asDynamic()[0])
    if (cType != "number") return "fail9: $cType"

    c[0] = 'W'
    val cType2 = jsTypeOf(c.asDynamic()[0])
    if (cType2 != "number") return "fail10: $cType2"

    val cType3 = jsTypeOf(c[0].asDynamic())
    if (cType3 != "number") return "fail11: $cType3"

    if (c.asDynamic().constructor.name != "Uint16Array") return "fail12: ${c.asDynamic().constructor.name}"

    return "OK"
}
