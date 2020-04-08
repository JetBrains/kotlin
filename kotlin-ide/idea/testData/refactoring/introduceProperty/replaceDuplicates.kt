// EXTRACTION_TARGET: property with initializer

class A(val n: Int) {
    fun foo(k: Int) {
        val a = <selection>n + 1</selection>
        val b = n + 1 - 2
        val c = foo(n + 1)
    }
}