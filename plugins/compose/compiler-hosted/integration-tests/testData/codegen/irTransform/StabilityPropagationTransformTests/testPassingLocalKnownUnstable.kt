import androidx.compose.runtime.Composable


import androidx.compose.runtime.remember

class Foo(var foo: Int)

@Composable
fun Test(x: Foo) {
    A(x)
    A(Foo(0))
    A(remember { Foo(0) })
}
