fun foo(a: Int): String {
    val x = "-${a + 1}"
    val y = "x${a + 1}y"
    val z = "x${a - 1}y"
    return "abc<selection>${a + 1}</selection>def"
}