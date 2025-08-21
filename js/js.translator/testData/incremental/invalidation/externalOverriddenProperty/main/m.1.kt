fun test(x: Foo) = "${x.value}K"

fun box(stepId: Int, isWasm: Boolean): String {
    return test(FooImpl("O"))
}
