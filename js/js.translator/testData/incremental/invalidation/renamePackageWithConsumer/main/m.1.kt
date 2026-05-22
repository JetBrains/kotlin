import sample.b.foo

fun box(stepId: Int, isWasm: Boolean): String {
    return if (foo() == 1) "OK" else "Fail step1"
}
