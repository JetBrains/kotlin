// EXPECTED_REACHABLE_NODES: 493
// ONLY_THIS_QUALIFIED_REFERENCES: foo_0

package foo

object A {
    private val foo = 23

    fun bar(): Int {
        return foo
    }
}

fun box(): String {
    var result = A.bar()
    if (result != 23) return "failed: ${result}"
    return "OK"
}