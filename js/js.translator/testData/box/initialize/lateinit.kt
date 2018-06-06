// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1120
class Greeting {
    val noon = xrun {
        verb = "Hello"
        "World"
    }

    lateinit var verb: String

    fun greet() = "$verb $noon"
}

fun <T> xrun(body: () -> T) = body()

fun box(): String {
    if (Greeting().greet() != "Hello World") return "fail"
    return "OK"
}