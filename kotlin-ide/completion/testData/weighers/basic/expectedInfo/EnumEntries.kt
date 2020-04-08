enum class EE {
    A,
    B
}

fun foo(): EE {
    return E<caret>
}

// ORDER: A
// ORDER: B
// ORDER: valueOf
// ORDER: EE
