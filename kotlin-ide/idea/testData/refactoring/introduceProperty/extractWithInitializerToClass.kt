// EXTRACTION_TARGET: property with initializer

class A(val n: Int = 1) {
    val m: Int = 2

    fun foo(): Int {
        return <selection>m + n + 1</selection>
    }
}

