// EXPECTED_REACHABLE_NODES: 1365
package foo

import kotlin.reflect.*

fun box(): String {
    try {
        "fail".unsafeCast<KClass<*>>().js
        return "fail try"
    } catch (cce: ClassCastException) {
        return "OK"
    }

    return "fail common"
}