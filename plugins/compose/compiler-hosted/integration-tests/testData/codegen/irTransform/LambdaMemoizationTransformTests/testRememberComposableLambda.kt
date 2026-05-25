import androidx.compose.runtime.*

@Composable
fun Test(s: String) {
  remember<@Composable () -> Unit> { { Text(s) } }()
  currentComposer.cache<@Composable () -> Unit>(false) { { Text(s) } }()
}
