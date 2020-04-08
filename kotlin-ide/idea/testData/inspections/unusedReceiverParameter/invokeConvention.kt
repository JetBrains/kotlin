// KT-7883 Receiver parameter falsely marked as unused

fun (() -> Any).foo() {
    this()
}