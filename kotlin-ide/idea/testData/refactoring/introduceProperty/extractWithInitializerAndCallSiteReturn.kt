// EXTRACTION_TARGET: property with initializer

class A(val n: Int = 1) {
    val m: Int = 2

    fun foo(): Int {
        val t = <selection>return m + n + 1</selection>
        return t
    }
}

