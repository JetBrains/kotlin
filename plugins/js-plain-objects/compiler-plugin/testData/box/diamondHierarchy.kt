// ISSUE: KT-87127

import kotlinx.js.JsPlainObject

@JsPlainObject
external interface A {
    val value: String
}

@JsPlainObject
external interface B: A

@JsPlainObject
external interface C: A

@JsPlainObject
external interface D: C, B

fun box(): String {
    val d = D(value = "Miku")

    if (d.value != "Miku") return "Fail: unexpected value ${d.value}"

    val json1 = js("JSON.stringify(d)")
    if (json1 != "{\"value\":\"Miku\"}") return "Fail: unexpected json $json1"

    val dCopy = D.copy(d, value = "Teto")
    if (dCopy.value != "Teto") return "Fail: unexpected value ${dCopy.value}"

    val json2 = js("JSON.stringify(dCopy)")
    if (json2 != "{\"value\":\"Teto\"}") return "Fail: unexpected json $json2"

    return "OK"
}
