interface I

fun foo() {
    abstract class T

    <selection>fun <A, B, C> bar1(a: A, b: B, c: C): I where B: A, C: I, C: T = c</selection>

    fun <X, Y: X, Z> bar2(x: X, y: Y, z: Z): I where Z: I, Z: T = z

    fun <X, Y, Z: I> bar3(x: X, y: Y, z: Z): T where Y: X, Z: T = z

    fun <X, Y: X, Z: I> bar4(x: X, y: Y, z: Z): T where Z: T = z

    fun <X, Y: X, Z: T> bar5(x: X, y: Y, z: Z): I where Z: I = z

    fun <X, Z: I, Y> bar6(x: X, y: Y, z: Z): I where Y: X, Z: T = z

    fun <X, Y: T, Z> bar7(x: X, y: Y, z: Z): T where Y: I, Z: I, Z: T = z
}