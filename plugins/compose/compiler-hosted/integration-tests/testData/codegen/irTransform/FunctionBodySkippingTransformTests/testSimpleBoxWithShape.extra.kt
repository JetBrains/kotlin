import androidx.compose.runtime.Composable


import androidx.compose.runtime.Stable

@Stable
interface Modifier {
  companion object : Modifier { }
}

interface Shape {
}

val RectangleShape = object : Shape { }

fun used(x: Any?) {}
