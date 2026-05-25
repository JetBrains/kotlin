import androidx.compose.runtime.Composable

class Foo
@Composable fun A(y: Int, x: Any) {
    used(y)
    B(x)
}
@Composable fun B(x: Any) {
    used(x)
}
