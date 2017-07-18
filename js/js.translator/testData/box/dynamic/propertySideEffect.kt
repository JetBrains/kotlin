// EXPECTED_REACHABLE_NODES: 992
external class C

inline val C.foo: String
    get() = asDynamic().foo

external val log: String

fun box(): String {
    val c = C()
    c.foo
    if (log != "foo called") return "fail: $log"
    return "OK"
}