import androidx.compose.runtime.*

@Composable
fun <T> Ref(content: @Composable (T) -> Unit) {
    Ref<T>(::Fn)
    Ref(::IntFn)
}
