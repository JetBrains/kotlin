// WITH_RUNTIME

import kotlinx.serialization.*

@Serializable
class UnsignedPrimitives(
    val byte: UByte,
    val short: UShort,
    val int: UInt,
    val long: ULong
)
