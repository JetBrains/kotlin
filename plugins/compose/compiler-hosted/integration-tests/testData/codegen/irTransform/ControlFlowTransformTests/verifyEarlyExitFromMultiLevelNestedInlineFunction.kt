import androidx.compose.runtime.*

@Composable
@NonRestartableComposable
fun Test(condition: Boolean) {
    Text("Before outer")
    InlineLinearA outer@{
        Text("Before inner")
        InlineLinearB {
            Text("Before return")
            if (condition) return@outer
            Text("After return")
        }
        Text("After inner")
    }
    Text("Before outer")
}
