import androidx.compose.runtime.*

@Composable
fun Ref(flag: Boolean) {
    Content(::Fn)
    Content(if (flag) ::Fn else ::Fn2)
}
