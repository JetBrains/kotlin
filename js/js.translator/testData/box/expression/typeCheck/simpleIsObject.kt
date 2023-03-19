// EXPECTED_REACHABLE_NODES: 1403
// IGNORE_BACKEND: JS

package foo

object RegularKotlinObject

@JsName("Array")
external object JsArray

@JsName("Math")
external object JsMath

fun box(): String {
    if (js("[]") !is JsArray) return "fail1"
    if (js("[]") is JsMath) return "fail2"
    if (js("[]") is RegularKotlinObject) return "fail3"
    if (JsMath !is JsMath) return "fail4"
    if (RegularKotlinObject !is RegularKotlinObject) return "fail5"

    return "OK"
}