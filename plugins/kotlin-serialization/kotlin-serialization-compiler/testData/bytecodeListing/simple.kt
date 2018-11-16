import kotlinx.serialization.*

@Serializable
class Simple(
    val value: String,
    @Optional val arg: Int = 42
)
