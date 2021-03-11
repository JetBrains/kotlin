// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "xcd$a"
    val y = "${a}cdx"
    val z = "xcf$a"
    return "ab<selection>cd</selection>ef"
}