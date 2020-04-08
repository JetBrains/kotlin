fun foo(a: Int): String {
    val x = "abc$a"
    val y = "abc${a}"
    val z = "abc{$a}def"
    return "<selection>abc$a</selection>" + "def"
}