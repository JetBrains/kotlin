fun foo(a: Int): String {
    val x = "_c$a:${a + 1}d_"
    val y = "_$a:${a + 1}d_"
    return "ab<selection>c$a:${a + 1}d</selection>ef"
}