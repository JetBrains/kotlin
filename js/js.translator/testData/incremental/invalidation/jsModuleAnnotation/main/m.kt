fun test(x: ObjectB<String, Int>?) = if (x == null) "OK" else "fail"

fun box(stepId: Int): String {
    return test(null)
}
