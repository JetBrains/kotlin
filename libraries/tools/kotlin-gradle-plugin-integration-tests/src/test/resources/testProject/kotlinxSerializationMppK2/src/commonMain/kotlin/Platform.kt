import kotlinx.serialization.Serializable

@Serializable
data class ValidateViolation(
    val filed: String,
)