fun returnFun(): Int = 10

fun usage(): Int {
    if (true) re<caret>

    return 10
}

// ORDER: return
// ORDER: returnFun