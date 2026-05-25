import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


class Foo {
 var counter: Int = 0
 @Composable fun A() {
    print("hello world")
 }
 @Composable fun B() {
    print(counter)
 }
}
