import androidx.compose.runtime.Composable


import androidx.compose.runtime.Stable

@Stable
interface Modifier {
  companion object : Modifier { }
}

fun used(x: Any?) {}
