fun returnFun(): Int = 10

fun usage(): Int {
    re<caret>
    return 10
}

// ORDER: returnFun
// ORDER: return
