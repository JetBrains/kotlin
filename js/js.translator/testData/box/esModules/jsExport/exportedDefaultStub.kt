// DONT_TARGET_EXACT_BACKEND: JS
// SKIP_DCE_DRIVEN
// SKIP_MINIFICATION
// ES_MODULES

@JsExport
fun ping(a: String = "A", b: Int = 1): String {
    return "$a::$b"
}

@JsExport
open class Ping(private val defaultSeed: Int = 3) {
    private fun calculate(n: Int) = n * n
    fun ping(s: Int = defaultSeed, c: (Int) -> Int = ::calculate): Int {
        return c(s)
    }
}

@JsExport
class Pong: Ping()

@JsExport
@JsName("pong")
fun bing(a: String = "A", b: Int = 1): String {
    return "$b::$a"
}

@JsExport
class Foo {
    @JsName("foo")
    fun value(value: Long = 5L) = if (value == 5L) "C" else "fail"
}

@JsExport
fun transform(i: Int = 10, t: (Int) -> Int = {it * it}): Int {
    return t(i)
}

external interface JsResult {
    val ping00: String
    val ping01: String
    val ping10: String
    val ping11: String

    val pong00: String
    val pong01: String
    val pong10: String
    val pong11: String

    val transform00: Int
    val transform11: Int

    val Ping_ping00a: Int
    val Ping_ping00b: Int
    val Ping_ping11: Int

    val Pong_ping00: Int

    val Foo: String
}

@JsModule("./exportedDefaultStub.mjs")
external fun jsBox(): JsResult

fun box(): String {
    val res = jsBox()
    if (res.ping00 != "A::1") {
        return "fail0: ${res.ping00}"
    }
    if (res.ping01 != "A::10") {
        return "fail1: ${res.ping01}"
    }
    if (res.ping10 != "X::1") {
        return "fail2: ${res.ping10}"
    }
    if (res.ping11 != "Z::5") {
        return "fail3: ${res.ping11}"
    }

    if (res.pong00 != "1::A") {
        return "fail4: ${res.pong00}"
    }
    if (res.pong01 != "10::A") {
        return "fail5: ${res.pong01}"
    }
    if (res.pong10 != "1::X") {
        return "fail6: ${res.pong10}"
    }
    if (res.pong11 != "5::Z") {
        return "fail7: ${res.pong11}"
    }

    if (res.transform00 != 100) {
        return "fail8: ${res.transform00}"
    }
    if (res.transform11 != -125) {
        return "fail9: ${res.transform11}"
    }

    if (res.Ping_ping00a != 9) {
        return "fail10: ${res.Ping_ping00a}"
    }
    if (res.Ping_ping00b != 100) {
        return "fail11: ${res.Ping_ping00b}"
    }
    if (res.Ping_ping11 != -64) {
        return "fail12: ${res.Ping_ping11}"
    }
    if (res.Pong_ping00 != 9) {
        return "fail13: ${res.Pong_ping00}"
    }

    if (res.Foo != "C") {
        return "fail14: ${res.Foo}"
    }

    return "OK"
}
