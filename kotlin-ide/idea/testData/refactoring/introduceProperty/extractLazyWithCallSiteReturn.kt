// EXTRACTION_TARGET: lazy property

class A(val n: Int = 1) {
    val m: Int = 2

    fun foo(): Int {
        <selection>return m + n + 1</selection>
    }
}

