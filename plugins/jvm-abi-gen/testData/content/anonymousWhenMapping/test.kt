package test

enum class E { A, B }

class Test {
    fun anonymousWhenMapping(e: E) = when (e) {
        E.A -> "OK"
        else -> "Fail"
    }
}
