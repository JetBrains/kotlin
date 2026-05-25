import androidx.compose.runtime.*

public inline fun <reified T : Any> NavGraphBuilder.bottomSheet(
  noinline dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
) {
}
