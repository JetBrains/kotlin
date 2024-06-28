// WITH_STDLIB
fun <T> jso(): T = mutableMapOf<String, Int>() as T

fun box(): String {
    val obj = jso<dynamic>()
    obj["a"] = 2
    ++obj["a"]
    return "OK"
}
