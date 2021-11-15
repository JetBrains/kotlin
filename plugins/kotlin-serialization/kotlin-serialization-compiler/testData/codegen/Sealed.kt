// CURIOUS_ABOUT deserialize, write$Self, childSerializers, <init>, <clinit>, invoke, serializer
// WITH_STDLIB

import kotlinx.serialization.*

// do not forget to update this test with custom serialinfo annotation when serialization 1.3.0 is released
@Serializable
sealed class Result {
    @Serializable class OK(val s: String): Result()
    @Serializable object Err: Result()
}

@Serializable
class Container(val r: Result)