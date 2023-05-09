import kotlinx.serialization.Serializable

fun callArrayOf(): Array<String> {
    return arrayOf("a", "b")
}

@Serializable
class ErrorResponse(
    val code: Int,
    val message: String,
)
