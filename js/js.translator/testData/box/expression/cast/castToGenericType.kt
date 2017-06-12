// EXPECTED_REACHABLE_NODES: 505
package foo

class A(val s: String)

fun <T> castsNotNullToNullableT(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun <T> castsNullableToNullableT(a: Any?) {
    a as T
    a as T?
    a as? T
    a as? T?
}


fun <T : Any> castsNotNullToNotNullT(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun <T : Any> castNullableToNotNullT(a: Any?) {
    a as T
}

fun <T : Any> castsNullableToNotNullT(a: Any?) {
    a as T?
    a as? T
    a as? T?
}

fun box(): String {
    val a = A("OK")

    success("castsNotNullToNullableT<A>(a)") { castsNotNullToNullableT<A>(a) }
    success("castsNullableToNullableT<A>(a)") { castsNullableToNullableT<A>(a) }
    success("castsNullableToNullableT<A>(null)") { castsNullableToNullableT<A>(null) }
    success("castsNotNullToNotNullT<A>(a)") { castsNotNullToNotNullT<A>(a) }
    success("castsNullableToNotNullT<A>(a)") { castsNullableToNotNullT<A>(a) }
    success("castsNullableToNotNullT<A>(null)") { castsNullableToNotNullT<A>(null) }
    success("castNullableToNotNullT<A>(a)") { castNullableToNotNullT<A>(a) }
    failsClassCast("castNullableToNotNullT<A>(null)") { castNullableToNotNullT<A>(null) }

    return "OK"
}
