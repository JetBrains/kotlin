// PROBLEM: none

fun test(p1: Operation, p2: Operation) {
    p1.<caret>plus(p2)
}

class Operation {
    fun plus(other: Operation) = 0
}