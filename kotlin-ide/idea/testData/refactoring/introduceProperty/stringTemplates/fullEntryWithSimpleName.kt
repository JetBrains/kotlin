// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "-$a"
    val y = "x${a}y"
    val z = "x$ay"
    return "abc<selection>${a}</selection>def"
}