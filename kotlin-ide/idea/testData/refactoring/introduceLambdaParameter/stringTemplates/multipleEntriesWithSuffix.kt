fun foo(a: Int): String {
    val x = "_ab$a:${a + 1}cd__"
    val y = "_a$a:${a + 1}cd__"
    return "<selection>ab$a:${a + 1}cd</selection>ef"
}