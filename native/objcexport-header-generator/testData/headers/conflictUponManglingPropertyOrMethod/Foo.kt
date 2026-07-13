class Foo {
    var NULL = 0  // When mangled, it clashes with the next property; should be handled by the mangler.
    val NULL_ = 1 // Should be mangled as NULL__ to avoid conflict with the previous property.

    fun YES() {}  // When mangled, it clashes with the next method; should be handled by the mangler.
    fun YES_() {} // Should be mangled as YES__ to avoid conflict with the previous method.

    fun NO() {}
    fun NO(x: Int, y: Long, z: Double) {}
    fun NOX() {}
}

fun DEBUG() {}
fun DE(BUG: Int) {}
