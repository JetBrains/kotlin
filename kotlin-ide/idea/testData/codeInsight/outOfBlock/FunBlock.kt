// OUT_OF_CODE_BLOCK: FALSE
fun run(block: () -> Int): Int {
    return block()
}

fun some(): Int = run { 12 + <caret> }

// TYPE: 1