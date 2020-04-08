fun returnFun() {}

fun usage(a: Int?): Int {
    a ?: re<caret>
    return 10
}

// ORDER: return
// ORDER: returnFun
