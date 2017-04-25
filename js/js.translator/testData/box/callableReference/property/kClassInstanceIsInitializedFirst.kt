// EXPECTED_REACHABLE_NODES: 497
// This test was adapted from compiler/testData/codegen/box/callableReference/property/.
package foo

import kotlin.reflect.KProperty1

class A {
    companion object {
        val ref: KProperty1<A, String> = A::foo
    }

    val foo: String = "OK"
}

fun box(): String {
    return A.ref.get(A())
}
