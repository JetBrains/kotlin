import androidx.compose.runtime.Composable

class A {
  @Composable fun B(x: Int) { }
}

@Composable
fun C() { A().B(1337) }
