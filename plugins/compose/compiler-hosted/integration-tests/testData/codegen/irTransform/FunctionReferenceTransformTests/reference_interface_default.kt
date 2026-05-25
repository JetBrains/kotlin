import androidx.compose.runtime.*

@Composable
fun Ref(intf: Intf, content: @Composable (Int) -> Unit) {
    Ref(intf, intf::Fn)
}
