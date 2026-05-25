import androidx.compose.runtime.Composable

@Composable
fun Test() {
  W {
    IW {
        T(2)
        repeat(3) {
            T(3)
        }
        T(4)
    }
  }
}
