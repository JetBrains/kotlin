// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1407
package foo

fun sequenceFromFunctionWithInitialValue() {
    val values = generateSequence(3) { n -> if (n > 0) n - 1 else null }
    assertEquals(arrayListOf(3, 2, 1, 0), values.toList())
}

fun box(): String {

    sequenceFromFunctionWithInitialValue()

    return "OK"
}