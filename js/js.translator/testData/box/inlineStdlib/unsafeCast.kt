// EXPECTED_REACHABLE_NODES: 487
package foo

// CHECK_NOT_CALLED_IN_SCOPE: scope=box function=isType
// CHECK_NOT_CALLED_IN_SCOPE: scope=box function=throwCCE

fun box(): String {
    // dynamic unsafeCast
    val result = js("\"OK\"").unsafeCast<String>()
    // Any unsafeCast
    val intOk = result.unsafeCast<Int>()

    return result
}