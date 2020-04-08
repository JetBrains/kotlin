fun foo(p: String) {
    if (x()) return
    println(p.<caret>isNotEmpty())
}
