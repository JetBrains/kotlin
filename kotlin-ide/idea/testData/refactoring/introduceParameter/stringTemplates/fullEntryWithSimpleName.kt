fun foo(a: Int): String {
    val x = "-$a"
    val y = "x${a}y"
    val z = "x$ay"
    return "abc<selection>${a}</selection>def"
}