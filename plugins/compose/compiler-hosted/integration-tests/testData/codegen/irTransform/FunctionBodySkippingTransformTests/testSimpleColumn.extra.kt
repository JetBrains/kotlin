import androidx.compose.runtime.Composable


import androidx.compose.runtime.Stable
import androidx.compose.runtime.Immutable

@Stable
interface Modifier {
  companion object : Modifier { }
}

@Immutable
interface Arrangement {
  @Immutable
  interface Vertical : Arrangement

  object Top : Vertical
}

enum class LayoutOrientation {
    Horizontal,
    Vertical
}

enum class SizeMode {
    Wrap,
    Expand
}

@Immutable
data class Alignment(
    val verticalBias: Float,
    val horizontalBias: Float
) {
    @Immutable
    data class Horizontal(val bias: Float)

    companion object {
      val Start = Alignment.Horizontal(-1f)
    }
}

fun used(x: Any?) {}
