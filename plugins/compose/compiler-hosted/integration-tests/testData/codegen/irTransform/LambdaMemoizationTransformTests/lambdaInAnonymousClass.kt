import androidx.compose.runtime.Composable

@Composable
fun test() {
  val foo =
    object {
      val bar = run { {} }
    }
}
