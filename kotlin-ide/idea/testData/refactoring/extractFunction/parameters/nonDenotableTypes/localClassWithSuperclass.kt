interface T

fun foo(): T {
    class A: T

    // SIBLING:
    fun bar(): T {
        return <selection>A()</selection>
    }
}