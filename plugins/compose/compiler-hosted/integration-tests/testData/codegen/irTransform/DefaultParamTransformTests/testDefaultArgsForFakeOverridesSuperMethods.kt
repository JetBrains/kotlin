import androidx.compose.runtime.*


open class Foo {
    @NonRestartableComposable @Composable fun foo(x: Int = 0) {}
}
class Bar: Foo() {
    @NonRestartableComposable @Composable fun Example() {
        foo()
    }
}
