// WITH_STDLIB

import kotlin.test.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Holder(val nested: Validated)

@Serializable
data class Validated(val value: Int) {
    init {
        require(value > 0) { "value must be positive" }
    }
}

@Serializable
data class Preserved(val value: Int) {
    init {
        throw SerializationException("original failure")
    }
}

fun box(): String {
    val wrapped = assertFailsWith<SerializationException> {
        Json.decodeFromString<Holder>("""{"nested":{"value":0}}""")
    }
    val wrappedMessage = wrapped.message ?: return "FAIL: no wrapped message"
    if (!wrappedMessage.contains("Failed to construct") || !wrappedMessage.contains("Validated")) {
        return "FAIL: unexpected wrapped message: $wrappedMessage"
    }
    val wrappedCause = wrapped.cause ?: return "FAIL: no wrapped cause"
    if (wrappedCause !is IllegalArgumentException) {
        return "FAIL: unexpected wrapped cause: ${wrappedCause::class}"
    }
    if (wrappedCause.message != "value must be positive") {
        return "FAIL: unexpected wrapped cause message: ${wrappedCause.message}"
    }

    val preserved = assertFailsWith<SerializationException> {
        Json.decodeFromString<Preserved>("""{"value":1}""")
    }
    if (preserved.message != "original failure") {
        return "FAIL: SerializationException was wrapped: ${preserved.message}"
    }

    return "OK"
}
