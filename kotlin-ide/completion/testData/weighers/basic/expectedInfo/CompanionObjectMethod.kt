class CC {
    companion object {
        fun getInstance(): CC {}
    }
}

fun foo(): CC {
    return C<caret>
}

// ORDER: CC
// ORDER: getInstance
