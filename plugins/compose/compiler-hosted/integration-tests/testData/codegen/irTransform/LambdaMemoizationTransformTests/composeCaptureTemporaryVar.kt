import androidx.compose.runtime.Composable

data class ChatRequestConfig(val a: Int = 10)

@Composable
fun TextField(
    onValueChange: (String) -> Unit
) {}

@Composable
fun App() {
    val currentRequestConfig = ChatRequestConfig(321)
    fun updateRequestConfig() {
        val config = ChatRequestConfig(currentRequestConfig.a ?: 10)
    }

    TextField(
        onValueChange = {
            updateRequestConfig()
        }
    )
}
