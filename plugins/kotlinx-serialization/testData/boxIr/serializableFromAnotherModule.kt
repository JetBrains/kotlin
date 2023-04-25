// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-57626

// MODULE: lib
import kotlinx.serialization.*

@Serializable
data class A(val s: String = "")

// MODULE: main(lib)
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class B(val a: A? = null)

fun box(): String {
    val expectedB = B(A("OK"))
    val json = Json.encodeToString(B.serializer(), expectedB)
    if (json != """{"a":{"s":"OK"}}""") return "Fail: $json"
    val actualB = Json.decodeFromString(B.serializer(), json)
    if (expectedB != actualB) return "Fail: $actualB"
    return "OK"
}
