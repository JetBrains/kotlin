fun returnFun(): Int = 10

fun usage(a: Int): Int {
    when (a) {
        10 -> {
            re<caret>
        }
    }
    return 10
}

// ORDER: return
// ORDER: returnFun
