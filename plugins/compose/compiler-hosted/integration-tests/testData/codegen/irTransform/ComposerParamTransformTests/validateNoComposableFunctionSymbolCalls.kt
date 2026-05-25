@file:OptIn(
              InternalComposeApi::class,
            )
            package test

            import androidx.compose.runtime.InternalComposeApi
            import androidx.compose.runtime.ComposeCompilerApi
            import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable

            fun abc0(l: @Composable () -> Unit) {
    val hc = l.hashCode()
}
fun abc1(l: @Composable (String) -> Unit) {
    val hc = l.hashCode()
}
fun abc2(l: @Composable (String, Int) -> Unit) {
    val hc = l.hashCode()
}
fun abc3(
    l: @Composable (Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any, Any) -> Any
) {
    val hc = l.hashCode()
}
