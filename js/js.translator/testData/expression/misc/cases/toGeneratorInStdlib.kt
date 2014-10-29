package foo

fun streamFromFunctionWithInitialValue() {
    val values = stream(3) { n -> if (n > 0) n - 1 else null }
    assertEquals(arrayListOf(3, 2, 1, 0), values.toList())
}

fun iterateOverFunction() {
    val values = iterate<Int>(3) { n -> if (n > 0) n - 1 else null }
    assertEquals(arrayList(3, 2, 1, 0), values.toList())
}

fun box(): String {

    streamFromFunctionWithInitialValue()
    iterateOverFunction()

    return "OK"
}