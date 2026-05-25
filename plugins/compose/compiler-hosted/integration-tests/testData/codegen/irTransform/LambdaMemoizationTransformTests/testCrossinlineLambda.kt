import androidx.compose.runtime.Composable

@Composable
fun Test() {
  var lambda: (@Composable () -> Unit)? = null
  f { s -> lambda = { Text(s) } }
  lambda?.let { it() }
}
