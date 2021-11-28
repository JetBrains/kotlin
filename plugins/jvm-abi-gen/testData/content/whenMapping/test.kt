package test

enum class E { A, B }

class Test {
    inline fun visibleWhenMapping(e: E) = when (e) {
        E.A -> "OK"
        else -> "Fail"
    }
}
