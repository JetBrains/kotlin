// FIR_COMPARISON
class A

typealias TA = A
typealias TA1 = A

fun TA.ext() {}

fun usage() {
    val ta = TA1()
    ta.<caret>
}

// EXIST: ext