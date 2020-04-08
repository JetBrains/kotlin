// SIBLING:
fun foo() {
    open class X(val x: Int)

    fun <T> bar(t: T): Int where T: X {
        return <selection>t.x + 1</selection>
    }
}