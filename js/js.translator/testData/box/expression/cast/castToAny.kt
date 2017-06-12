// EXPECTED_REACHABLE_NODES: 507
package foo

class A

fun box(): String {
    val nil: A? = null
    val a = A()

    success("10 as Any") { assertEquals<Any>(10, 10 as Any) }
    success("\"abc\" as Any") { assertEquals<Any>("abc", "abc" as Any) }
    success("\"abc\" as Any") { assertEquals<Any>(true, true as Any) }
    val array = arrayOf(1, 2)
    success("arrayOf(1, 2) as Any") { assertEquals<Any>(array, array as Any) }
    success("{ 0 } as Any") { { 0 } as Any }
    success("a as Any") { assertEquals<Any>(a, a as Any) }
    success("object{} as Any") { object{} as Any }
    failsClassCast("nil as Any") { nil as Any }

    return "OK"
}