import androidx.compose.runtime.*

@Composable
fun Ref(flag: Boolean, content: @Composable (Int) -> Int) {
    Ref(flag, if (flag) ::Fn else ::Fn2)
}
