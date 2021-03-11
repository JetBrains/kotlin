// EXTRACTION_TARGET: property with initializer
fun foo(): String {
    val x = "atrue123"
    val x = "aTRUE123"
    val z = true
    return "ab<selection>true</selection>def"
}