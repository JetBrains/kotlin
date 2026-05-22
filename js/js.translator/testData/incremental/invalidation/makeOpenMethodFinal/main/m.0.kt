fun box(stepId: Int, isWasm: Boolean): String {
    val actual = use(FooImpl())
    return if (actual == 42) "OK" else "Fail: $actual"
}
