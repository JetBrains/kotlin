package test

object Conflict {
    operator fun invoke() {}
}

fun test() {
    class Conflict

    <caret>Conflict()
}

// REF: (in test.test).Conflict