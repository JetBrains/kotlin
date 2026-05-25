import androidx.compose.runtime.*

@Composable
@NonRestartableComposable
fun Test(condition: Boolean) {
    Text("Before outer")
    InlineLinearA {
        Text("Before inner")
        InlineLinearB inner@{
            Text("Before return")
            if (condition) return@inner
            Text("After return")
        }
        Text("After inner")
    }
    Text("Before outer")
}
