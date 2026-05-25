// LANGUAGE: +ContextParameters
import androidx.compose.runtime.*

@Composable
fun <T : Any> TypeCrossfade(
    state: T,
    content: @Composable context(BoxScope) T.() -> Unit
) {
    Crossfade(state) {
        with(BoxScope) {
            content(it)
        }
    }
}

@Composable fun App() {
    TypeCrossfade(States.A()) {
        Text(this.toString())
    }
}
