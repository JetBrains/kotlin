// IGNORE_FIR

package test

object Conflict {
    operator fun invoke() {}
}

fun test() {
    class Conflict(i: Int)

    <caret>Conflict()
}

// REF: (test).Conflict