enum class E(val value: String) {
    E1(A.<caret>Companion.foo);
}

class A {
    companion object {
        const val foo = ""
    }
}