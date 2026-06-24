// WITH_STDLIB
// API_VERSION: LATEST
// OPT_IN: kotlin.time.ExperimentalTime

// FILE: test.kt

package a

import kotlin.time.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Holder(val i: Instant)

fun box(): String {
    val h = Holder(Instant.parse("2025-01-04T23:59:14.0001242Z"))
    val msg = Json.encodeToString(h)
    return if (msg == """{"i":"2025-01-04T23:59:14.000124200Z"}""") "OK" else "FAIL: $msg"
}

