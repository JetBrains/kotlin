fun test(x: ObjectB<String, Int>?) = if (x == null) "OK" else "fail"

fun box(stepId: Int, isWasm: Boolean): String {
    return test(null)
}
