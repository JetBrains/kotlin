package foo

class A(val s: String)

fun castsNotNullToNullableT<T>(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun castsNullableToNullableT<T>(a: Any?) {
    a as T
    a as T?
    a as? T
    a as? T?
}


fun castsNotNullToNotNullT<T : Any>(a: Any) {
    a as T
    a as T?
    a as? T
    a as? T?
}

fun castNullableToNotNullT<T : Any>(a: Any?) {
    a as T
}

fun castsNullableToNotNullT<T : Any>(a: Any?) {
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

fun fails(f: () -> Unit) {
    try {
        f()
    }
    catch(e: Exception) {
        return
    }

    throw Exception("Expected an exception to be thrown from $f")
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

    return "OK"
}
