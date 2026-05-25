import androidx.compose.runtime.Composable
            import androidx.compose.runtime.NonRestartableComposable
            import androidx.compose.runtime.ReadOnlyComposable

            import androidx.compose.runtime.NonSkippableComposable

@Composable
@NonSkippableComposable
fun Test(i: Int) {
    used(i)
}
