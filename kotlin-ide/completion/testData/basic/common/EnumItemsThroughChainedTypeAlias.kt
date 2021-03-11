enum class A {
    ONE;
    class B // Not allowed to resolve through typealiases
}

typealias AA = A
typealias AAA = AA

fun usage() {
    AAA.<caret>
}

// EXIST: ONE
// ABSENT: B