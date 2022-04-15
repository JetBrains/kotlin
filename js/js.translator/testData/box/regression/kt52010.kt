// EXPECTED_REACHABLE_NODES: 1274

fun box(): String {
    instance = Holder()
    instance?.applyAndRet<Unit> { sideEffect() } ?: sideEffect()

    if (sideEffectCnt == 1) return "OK" else return "fail"
}

var sideEffectCnt = 0

fun sideEffect() {
    sideEffectCnt++
}

var instance: Holder? = null

class Holder() {
    fun <T> applyAndRet(block: () -> T): T {
        return block()
    }
}