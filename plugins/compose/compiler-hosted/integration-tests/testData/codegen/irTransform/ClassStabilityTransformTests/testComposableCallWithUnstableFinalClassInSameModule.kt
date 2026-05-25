import androidx.compose.runtime.Composable

class Foo(var bar: Int = 0)
@Composable fun A(y: Int, x: Foo) {
    used(y)
    B(x)
}
@Composable fun B(x: Any) {
    used(x)
}
