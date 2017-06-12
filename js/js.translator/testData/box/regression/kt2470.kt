// EXPECTED_REACHABLE_NODES: 493
// KT-2470 another name mangling bug: kotlin.test.failsWith() gets generated to invalid JS

package foo

public inline fun <reified T : Throwable> failsWith(block: () -> Any): T {
    try {
        block()
    }
    catch (e: Throwable) {
        if (e is T) return e
    }

    throw Exception("Should have failed")
}

fun box(): String {
    val a = failsWith<Exception> {
        throw Exception("OK")
    }

    return a.message!!
}
