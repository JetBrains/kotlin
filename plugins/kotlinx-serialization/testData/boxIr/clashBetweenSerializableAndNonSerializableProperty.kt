// ISSUE: KT-64312
// WITH_STDLIB
// MODULE: lib
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
abstract class Base {
    @SerialName("properties")
    protected var propertiesInternal: String? = null

    val properties: String // should not be serialized because it does not have a backing field
        get() = propertiesInternal?.reversed().orEmpty()
}

// MODULE: main(lib)
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Derived: Base() {
    fun initialize() {
        propertiesInternal = "KO"
    }
}

fun box(): String {
    val expected = Derived().also { it.initialize() }
    val json = Json.encodeToString(Derived.serializer(), expected)
    if (json != """{"properties":"KO"}""") return "Fail: $json"
    val actual = Json.decodeFromString(Derived.serializer(), json)
    if (expected.properties != actual.properties) return "Fail: $actual"
    return actual.properties
}
