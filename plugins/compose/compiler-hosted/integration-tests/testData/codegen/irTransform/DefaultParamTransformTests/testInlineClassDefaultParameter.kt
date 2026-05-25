import androidx.compose.runtime.*


@Composable
fun Example(foo: Foo = Foo(0)) {
    print(foo)
}
@Composable
fun Test() {
    Example()
}
