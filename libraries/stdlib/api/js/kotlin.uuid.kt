@kotlin.SinceKotlin(version = "2.0")
@kotlin.ExperimentalStdlibApi
public final class Uuid : kotlin.io.Serializable {
    public open override operator fun equals(other: kotlin.Any?): kotlin.Boolean

    public open override fun hashCode(): kotlin.Int

    public final fun toByteArray(): kotlin.ByteArray

    public final fun toHexString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun <T> toLongs(action: (mostSignificantBits: kotlin.Long, leastSignificantBits: kotlin.Long) -> T): T

    public open override fun toString(): kotlin.String

    @kotlin.internal.InlineOnly
    public final inline fun <T> toULongs(action: (mostSignificantBits: kotlin.ULong, leastSignificantBits: kotlin.ULong) -> T): T

    public companion object of Uuid {
        public final val LEXICAL_ORDER: kotlin.Comparator<kotlin.uuid.Uuid> { get; }

        public final val NIL: kotlin.uuid.Uuid { get; }

        public const final val SIZE_BITS: kotlin.Int = 128 { get; }

        public const final val SIZE_BYTES: kotlin.Int = 16 { get; }

        public final fun fromByteArray(byteArray: kotlin.ByteArray): kotlin.uuid.Uuid

        public final fun fromLongs(mostSignificantBits: kotlin.Long, leastSignificantBits: kotlin.Long): kotlin.uuid.Uuid

        public final fun fromULongs(mostSignificantBits: kotlin.ULong, leastSignificantBits: kotlin.ULong): kotlin.uuid.Uuid

        public final fun parse(uuidString: kotlin.String): kotlin.uuid.Uuid

        public final fun parseHex(hexString: kotlin.String): kotlin.uuid.Uuid

        public final fun random(): kotlin.uuid.Uuid
    }
}