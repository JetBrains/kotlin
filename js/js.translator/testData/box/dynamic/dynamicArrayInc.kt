// ISSUE: KT-65195, KT-87887
// IGNORE_BACKEND: JS_IR, JS_IR_ES6

fun jso(): dynamic = js("{}")

fun box(): String {
    val obj = jso()
    obj["a"] = 2
    ++obj["a"]
    if (obj["a"] != 3) return "Fail: prefix increment: ${obj["a"]}"
    obj["a"]++
    if (obj["a"] != 4) return "Fail: postfix increment: ${obj["a"]}"
    --obj["a"]
    if (obj["a"] != 3) return "Fail: prefix decrement: ${obj["a"]}"
    obj["a"]--
    if (obj["a"] != 2) return "Fail: postfix decrement: ${obj["a"]}"
    return "OK"
}
