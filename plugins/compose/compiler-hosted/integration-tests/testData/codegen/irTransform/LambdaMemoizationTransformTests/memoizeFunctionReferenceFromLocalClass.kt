import androidx.compose.runtime.Composable

class MainActivity {
  private val test = object : Test {
    override fun go() {
      this@MainActivity
    }
  }

  @Composable fun Test() {
    test::go
  }
}
