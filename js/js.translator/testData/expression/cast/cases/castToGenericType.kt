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

    castsNotNullToNullableT<A>(a)
    castsNullableToNullableT<A>(a)
    castsNullableToNullableT<A>(null)
    castsNotNullToNotNullT<A>(a)
    castsNullableToNotNullT<A>(a)
    castsNullableToNotNullT<A>(null)
    castNullableToNotNullT<A>(a)
    failsClassCast("castNullableToNotNullT<A>(null)") { castNullableToNotNullT<A>(null) }

    return "OK"
}
