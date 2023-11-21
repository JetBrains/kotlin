// ISSUE: KT-63593
// WITH_STDLIB

fun <T> jso(): T = mutableMapOf<String, String>() as T

fun box(): String {
    val obj = jso<dynamic>()
    obj["a"] = run { "OK" }
    return obj["a"]
}
