import androidx.compose.runtime.*


@NonRestartableComposable
@Composable
fun Bar() {
    HasDefault()
    NoDefault("Some Text")
    MultipleDefault()
}
