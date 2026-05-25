import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*

@Composable
fun Test(count: Int) {
    Row {
        repeat(count) {
            Text("A")
        }
    }
}
