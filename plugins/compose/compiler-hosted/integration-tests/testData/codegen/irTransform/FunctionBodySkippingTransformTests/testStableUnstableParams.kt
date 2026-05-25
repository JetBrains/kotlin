import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo(var value: Int = 0)

@Composable fun CanSkip(a: Int = 0, b: Foo = Foo()) {
    used(a)
    used(b)
}
@Composable fun CannotSkip(a: Int, b: Foo) {
    used(a)
    used(b)
    print("Hello World")
}
@Composable fun NoParams() {
    print("Hello World")
}
