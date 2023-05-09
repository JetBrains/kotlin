// EXPECTED_REACHABLE_NODES: 1286
// MODULE: lib
// FILE: lib.kt

class C<V>(val v: V) {

    fun <R1, R2: List<R1>> reduce(reducer: (reduction: V) -> R2) {}
    fun <R1, R2: List<R1>> reduce(reducer: (reduction: R1) -> R2) {}
}

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val c = C(42)

    return "OK"
}
