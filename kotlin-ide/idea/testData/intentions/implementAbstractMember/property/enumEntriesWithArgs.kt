// WITH_RUNTIME
//DISABLE-ERRORS
enum class E(n: Int) {
    A(1), B(2), C(3);

    abstract val <caret>foo: Int
}