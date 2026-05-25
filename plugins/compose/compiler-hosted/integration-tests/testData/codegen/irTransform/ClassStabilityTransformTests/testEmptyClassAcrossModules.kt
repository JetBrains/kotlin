import a.*
import androidx.compose.runtime.Composable

@Composable fun A(y: Any) {
    used(y)
    A(Wrapper(Foo()))
}
