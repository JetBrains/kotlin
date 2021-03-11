// EXTRACTION_TARGET: property with initializer
val a = 1

fun foo(): String {
    val x = "abc$a"
    val y = "abc${a}"
    val z = "abc{$a}def"
    return "<selection>abc$a</selection>" + "def"
}