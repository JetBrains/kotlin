class Declaration {
    fun lambdaType(p: Int, f: (Int) -> Int) = f(p)
}

fun call(declaration: Declaration) {
    declaration.<caret>lambdaType(10) { _ -> 11 }
}