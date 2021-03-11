// PARAM_DESCRIPTOR: val a: A defined in foo.bar
// PARAM_TYPES: A, T
interface T

fun foo(): T {
    class A: T

    // SIBLING:
    fun bar(): T {
        val a = A()
        return <selection>a</selection>
    }
}