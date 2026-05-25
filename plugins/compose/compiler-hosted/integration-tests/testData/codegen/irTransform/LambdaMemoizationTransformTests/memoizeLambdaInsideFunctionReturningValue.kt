import androidx.compose.runtime.Composable

@Composable
fun Test(foo: Foo): Int =
  Consume { foo.value }
