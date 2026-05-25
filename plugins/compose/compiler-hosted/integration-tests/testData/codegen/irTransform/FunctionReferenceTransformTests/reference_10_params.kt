// IGNORE_BACKEND_K2: JVM_IR
import androidx.compose.runtime.*

@Composable
fun Ref(content: @Composable (
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int,
    Int
) -> Unit) {
    Ref(::Fn)
}
