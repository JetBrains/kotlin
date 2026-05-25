import a.*
import androidx.compose.runtime.Composable

@Composable fun <V> B(value: V) {
    A(Wrapper(value))
}
@Composable fun <T> X(items: List<T>, itemContent: @Composable (T) -> Unit) {
    for (item in items) itemContent(item)
}
@Composable fun C(items: List<String>) {
    X(items) { item ->
        A(item)
        A(Wrapper(item))
    }
}
