// WITH_STDLIB
// ISSUE: KT-54994

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.internal.*

const val prefix = "foo"

@Serializable
data class Bar(@SerialName("$prefix.bar") val bar: String)

fun box(): String {
    val expectedBar = Bar("hello")
    val json = Json.encodeToString(Bar.serializer(), expectedBar)
    if (json != """{"foo.bar":"hello"}""") return "Fail: $json"
    val actualBar = Json.decodeFromString(Bar.serializer(), json)
    if (expectedBar != actualBar) return "Fail: $actualBar"
    return "OK"
}
