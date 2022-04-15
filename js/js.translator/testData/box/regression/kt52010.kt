// EXPECTED_REACHABLE_NODES: 1274

fun box(): String {
    instance = Holder()
    instance?.applyAndRet<Unit> { sideEffect("left") } ?: sideEffect("right")

    if (log == "left") return "OK" else return "fail: $log"
}

var log = ""

fun sideEffect(msg: String) {
    log += msg
}

var instance: Holder? = null

class Holder() {
    fun <T> applyAndRet(block: () -> T): T {
        return block()
    }
}
