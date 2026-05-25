import androidx.compose.runtime.*

@Composable fun NonInlined() {
   val a = @Composable { }
}

@Composable inline fun Inlined() {
   val b = @Composable {}
}
