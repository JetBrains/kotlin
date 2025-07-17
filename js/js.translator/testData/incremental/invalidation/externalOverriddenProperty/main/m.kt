fun test(x: Foo) = x.value

fun box(stepId: Int, isWasm: Boolean): String {
    return test(FooImpl("OK"))
}
