import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.ExplicitGroupsComposable

@Composable
@ExplicitGroupsComposable
fun A(foo: Foo) {
    foo.b()
}

@Composable
@ExplicitGroupsComposable
inline fun Foo.b(label: String = "") {
    c(this, label)
}

@Composable
@ExplicitGroupsComposable
inline fun c(foo: Foo, label: String) {
    print(label)
}
