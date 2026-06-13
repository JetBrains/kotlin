import sample.a.foo

fun box(stepId: Int, isWasm: Boolean): String {
    return if (foo() == 0) "OK" else "Fail step0"
}
