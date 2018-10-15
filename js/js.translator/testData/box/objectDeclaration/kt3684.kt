// EXPECTED_REACHABLE_NODES: 1292
// copied from JVM backend tests
package foo

open class X(private val n: String) {

    fun foo(): String {
        return object : X("inner") {
            fun print(): String {
                return n;
            }
        }.print()
    }
}


fun box(): String {
  return X("OK").foo()
}
