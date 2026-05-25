import androidx.compose.runtime.Composable

@Composable
fun test() {
    class Foo {
      val bar = run { {} }
    }
}
