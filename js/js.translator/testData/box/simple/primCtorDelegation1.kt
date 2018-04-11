// EXPECTED_REACHABLE_NODES: 1114
package foo

open class Base(val bb: String) {

    open fun foo() = bb
}

class Test(val tt: String) : Base("fail") {

    override fun foo() = tt

}

fun box(): String {

    val t = Test("OK")
    return t.foo()
}