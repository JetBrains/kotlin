data class Declaration(val x: Int, val y: Int) {
    fun lambdaType(p: Declaration, f: (Declaration) -> Int) = f(p)
}

fun call(declaration: Declaration) {
    declaration.lambdaType(declaration) {<caret> (x, y) -> 11 }
}