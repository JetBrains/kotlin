// WITH_STDLIB

package com.example

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*


class NonSerializable {
    companion object {
        fun foo(): String {
            return "OK"
        }
    }
}

@Serializable
data class WithCompanion(val i: Int) {
    companion object {
        fun foo(): String {
            return "OK"
        }
    }
}

@Serializable
data class WithNamedCompanion(val i: Int) {
    companion object Named {
        fun foo(): String {
            return "OK"
        }
    }
}


fun box(): String {
    encodeAndDecode(WithCompanion.serializer(), WithCompanion(1), """{"i":1}""")?.let { return it }
    encodeAndDecode(WithNamedCompanion.serializer(), WithNamedCompanion(2), """{"i":2}""")?.let { return it }
    if (NonSerializable.foo() != "OK") return NonSerializable.foo()
    if (WithCompanion.foo() != "OK") return WithCompanion.foo()
    if (WithNamedCompanion.foo() != "OK") return WithNamedCompanion.foo()

    return "OK"
}


private fun <T> encodeAndDecode(serializer: KSerializer<T>, value: T, expectedEncoded: String, expectedDecoded: T? = null): String? {
    val encoded = Json.encodeToString(serializer, value)
    if (encoded != expectedEncoded) return encoded

    val decoded = Json.decodeFromString(serializer, encoded)
    if (decoded != (expectedDecoded ?: value)) return "DECODED=${decoded.toString()}"
    return null
}
