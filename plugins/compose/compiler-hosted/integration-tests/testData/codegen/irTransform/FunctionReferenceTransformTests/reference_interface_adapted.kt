import androidx.compose.runtime.*

@Composable
fun Ref(intf: Intf, content: @Composable () -> Unit) {
    Ref(intf, intf::Fn)
}
