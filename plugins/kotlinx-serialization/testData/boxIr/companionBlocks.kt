// LANGUAGE: +CompanionBlocks +CompanionExtensions
// TARGET_BACKEND: JVM_IR
// JS: KT-85459, Native: KT-84829

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.builtins.*

@Serializable
data class Vector(val x: Double, val y: Double) {
    companion {
        val UnitX = Vector(1.0, 0.0)

        val json = Json { encodeDefaults = false }

        fun serializerAccess(): KSerializer<Vector> = Vector.serializer()
    }
}

@Serializable
data class Box<T>(val t: T) {
    companion {
        val nullBox: Box<Any?> = Box(null)
    }
}

@Serializable
data class InitFromCompanion(val a: Int = defaultA) {
    companion {
        val defaultA = 42
    }
}

@Serializable
data class SerializerDeclared(val a: Int) {
    companion {
        // companion block functions have priority over companion object functions
        fun serializer() = "serializer"
        fun serializerSignature(): KSerializer<SerializerDeclared> = SerializerDeclared.Companion.serializer()
    }
}

fun testSerializerDeclared(): String {
    if (SerializerDeclared.serializer() != "serializer") return "FAIL priority"
    return boxTest(SerializerDeclared(10), SerializerDeclared.Companion.serializer(), """{"a":10}""", 1)
}

fun testInit(): String {
    return boxTest(InitFromCompanion(), InitFromCompanion.serializer(), """{}""", 1)
}

fun <T> boxTest(t: T, kSerializer: KSerializer<T>, expected: String, descCount: Int): String {
    val encoded = Vector.json.encodeToString(kSerializer, t)
    if (encoded != expected) return "FAIL encoded: $encoded expected: $expected"
    val decoded = Vector.json.decodeFromString(kSerializer, encoded)
    if (decoded != t) return "FAIL decoded: $decoded expected: $t"
    val cnt = kSerializer.descriptor.elementsCount
    if (cnt != descCount) return "FAIL element count: $cnt expected: $descCount"
    return "OK"
}

fun box(): String {
    val vector = boxTest(Vector.UnitX, Vector.serializer(), """{"x":1.0,"y":0.0}""", 2)
    if (vector != "OK") return vector
    val vectorAccess = boxTest(Vector.UnitX, Vector.serializerAccess(), """{"x":1.0,"y":0.0}""", 2)
    if (vectorAccess != "OK") return vectorAccess
    val box = boxTest(Box("abc"), Box.serializer(String.serializer()), """{"t":"abc"}""", 1)
    if (box != "OK") return box
    val init = testInit()
    if (init != "OK") return init
    return testSerializerDeclared()
}
