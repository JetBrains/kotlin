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

fun test(f: () -> Unit) {
    try {
        f()
    }
    catch(e: Exception) {
        throw Exception("Failed in $f with unexpected exception $e")
    }
}

fun box(): String {
    val a = A("OK")

    test { castsNotNullToNullableT<A>(a) }

    test { castsNullableToNullableT<A>(a) }
    test { castsNullableToNullableT<A>(null) }

    test { castsNotNullToNotNullT<A>(a) }

    test { castsNullableToNotNullT<A>(a) }
    test { castsNullableToNotNullT<A>(null) }

    test  { castNullableToNotNullT<A>(a) }
    fails { castNullableToNotNullT<A>(null) }
    failsClassCast("castNullableToNotNullT<A>(null)") { castNullableToNotNullT<A>(null) }

    return "OK"
}
