class Declaration {
    fun lambdaType(p: Int, f: (Int) -> Int) = f(p)
}

fun call(declaration: Declaration) {
    val a = declaration.<caret>lambdaType(6) { it + 7 }
}