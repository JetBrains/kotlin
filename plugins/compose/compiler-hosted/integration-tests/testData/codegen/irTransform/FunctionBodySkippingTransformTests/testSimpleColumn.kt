import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


@Composable
fun RowColumnImpl(
  orientation: LayoutOrientation,
  modifier: Modifier = Modifier,
  arrangement: Arrangement.Vertical = Arrangement.Top,
  crossAxisAlignment: Alignment.Horizontal = Alignment.Start,
  crossAxisSize: SizeMode = SizeMode.Wrap,
  content: @Composable() ()->Unit
) {
    used(orientation)
    used(modifier)
    used(arrangement)
    used(crossAxisAlignment)
    used(crossAxisSize)
    content()
}

@Composable
fun Column(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    horizontalGravity: Alignment.Horizontal = Alignment.Start,
    content: @Composable() ()->Unit
) {
  RowColumnImpl(
    orientation = LayoutOrientation.Vertical,
    arrangement = verticalArrangement,
    crossAxisAlignment = horizontalGravity,
    crossAxisSize = SizeMode.Wrap,
    modifier = modifier,
    content = content
  )
}
