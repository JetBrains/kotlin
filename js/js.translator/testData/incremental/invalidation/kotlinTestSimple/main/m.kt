fun box(stepId: Int, isWasm: Boolean): String {
    val expected = when (stepId) {
        0 -> "foo"
        1 -> "fooboo"
        2 -> "boo"
        else -> "UNEXPECTED STEP $stepId"
    }
    return if (expected == sharedValue) "OK" else "Expected $expected but actual $sharedValue"
}
