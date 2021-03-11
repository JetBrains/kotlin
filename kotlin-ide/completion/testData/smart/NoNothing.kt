fun fail() : Nothing{
    throw RuntimeException()
}

fun f(p : String) {
    var a : String = <caret>
}

// EXIST: p
// ABSENT: fail
// ABSENT: error
