// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "-${a + 1}"
    val y = "x${a + 1}y"
    val z = "x${a - 1}y"
    return "abc<selection>${a + 1}</selection>def"
}