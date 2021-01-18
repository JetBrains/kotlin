// EXPECTED_REACHABLE_NODES: 1303

// This hack is used in org.w3c.* part of standard library to represent unions of Strings
// Test that we are not actually trying to access nonexistent companion object

@JsName("null")
@Suppress("NESTED_CLASS_IN_EXTERNAL_INTERFACE")
public external interface I {
    companion object
}
public inline val I.Companion.O: I get() = "O".asDynamic().unsafeCast<I>()
public inline val I.Companion.K: I get() = "K".asDynamic().unsafeCast<I>()

fun box(): String {
    if (I.O != I.O) return "Fail 1"
    if (I.O == I.K) return "Fail 2"
    return I.O.unsafeCast<String>() + I.K.unsafeCast<String>()
}
