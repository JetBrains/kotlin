import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable


import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue

@Composable
fun StrongSkippingState() {
    val state by remember { mutableStateOf("") }; // <-- this is a load bearing ;
    { state }
}
