// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "+cd$a:${a + 1}efg"
    val y = "+cd$a${a + 1}efg"
    return "ab<selection>cd$a:${a + 1}ef</selection>"
}