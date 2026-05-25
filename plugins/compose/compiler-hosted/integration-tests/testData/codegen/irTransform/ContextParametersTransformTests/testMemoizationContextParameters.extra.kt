// LANGUAGE: +ContextParameters
import androidx.compose.runtime.Composable

sealed interface States {
    data class A(val i: Int = 0) : States
    data object B : States
}

@Composable fun Text(text: String) { }

object BoxScope

@Composable
fun <T : Any> Crossfade(
    state: T,
    content: @Composable (T) -> Unit
) {
}
