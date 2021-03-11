fun returnFun(): Int = 10

fun usage(): Int {
    if (true) {
        re<caret>
        return 20
    }

    return 10
}

// ORDER: returnFun
// ORDER: return
