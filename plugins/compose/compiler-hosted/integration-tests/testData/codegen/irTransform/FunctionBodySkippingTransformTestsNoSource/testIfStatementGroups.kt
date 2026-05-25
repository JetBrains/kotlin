import androidx.compose.runtime.*

@Composable
fun Test(level: Int) {
    Wrap {
        if (level > 0) {
            used(remember { "Before" })
            Wrap {
                used(remember { "Middle" })
            }
            used(remember { "End" })
        }
    }
}
