// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

@Serializable
sealed interface I

@Serializable
sealed class X: I {
    private var x = "X"

    fun exposeX() = x
}

@Serializable
abstract class Y: X() {
    private var y = "Y"

    fun exposeY() = y
}

@Serializable
class Bottom: Y()

fun testData(results: String): String {
    if (results != "OK") return results
    val serial = Bottom.serializer()
    val j = Json { encodeDefaults = true }
    val s = j.encodeToString(serial, Bottom())
    if (s != """{"x":"X","y":"Y"}""") return "Incorrect encoding: $s"
    val decoded = j.decodeFromString(serial, """{"x":"1","y":"2"}""")
    if (decoded.exposeX() != "1") return "Incorrect X"
    if (decoded.exposeY() != "2") return "Incorrect Y"
    return "OK"
}

@OptIn(ExperimentalSerializationApi::class)
fun testKinds(): String {
    if (I.serializer().descriptor.kind != PolymorphicKind.SEALED) return "Not sealed: I"
    if (X.serializer().descriptor.kind != PolymorphicKind.SEALED) return "Not sealed: X"
    if (Y.serializer().descriptor.kind != PolymorphicKind.OPEN) return "Not polymorphic: Y"
    val serial = Bottom.serializer()
    if (serial.descriptor.kind != StructureKind.CLASS) return "Not class: Bottom"
    return "OK"
}

fun box(): String {
    return testData(testKinds())
}
