// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*

@Serializable
data class SimpleDTO(
    val someString: String,
) {
    @Transient
    private val additionalProperties: Map<String, Int> = mapOf("someInt" to 123)
    @Transient
    val someInt: Int? by additionalProperties
}

fun box(): String {
    val dto = SimpleDTO("test")
    val s = Json.encodeToString(dto)
    val d = Json.decodeFromString<SimpleDTO>(s)
    if (s != """{"someString":"test"}""") return s
    if (d.someString != dto.someString) return "Deserialization failed"
    if (d.someInt !== 123) return "Delegate is incorrect!"
    return "OK"
}