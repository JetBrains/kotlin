// SIBLING:
fun main(args: Array<String>) {
    <selection>val a = 1</selection>
    val t = object: T {
        override fun foo(n: Int) = n + a
    }
}

interface T {
    fun foo(n: Int): Int
}