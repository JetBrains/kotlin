// KIND: STANDALONE
// MODULE: FunctionalType
// FILE: functional_type.kt

private var i: Int = 0

fun read(): Int = i

private var firstCall = true
fun produceClosureIncrementingI(): () -> Unit = if (firstCall) {
    firstCall = false
    { i += 1 }
} else {
    {}
}
