import androidx.compose.runtime.*

@Composable
fun <T> Ref(content: @Composable () -> T) {
    Ref<T>(::Fn)
    Ref(::IntFn)
}
