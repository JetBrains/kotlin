// WITH_STDLIB
// SKIP_TXT
import kotlinx.serialization.*


@Serializable
enum class ImplicitlyDuplicated {
    @SerialName("foo")
    FIRST,
    @SerialName("foo")
    SECOND
}

@Serializable
enum class ExplicitlyDuplicated {
    FIRST,
    SECOND,
    @SerialName("FIRST")
    THIRD
}

@Serializable
enum class ReversedExplicitlyDuplicated {
    @SerialName("THIRD")
    FIRST,
    SECOND,
    THIRD
}
