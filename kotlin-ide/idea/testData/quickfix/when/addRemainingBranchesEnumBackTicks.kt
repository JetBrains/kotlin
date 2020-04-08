// "Add remaining branches" "true"
// WITH_RUNTIME

enum class FooEnum {
    A, B, `C`, `true`, `false`, `null`
}

fun test(foo: FooEnum?) = <caret>when (foo) {
    FooEnum.A -> "A"
}