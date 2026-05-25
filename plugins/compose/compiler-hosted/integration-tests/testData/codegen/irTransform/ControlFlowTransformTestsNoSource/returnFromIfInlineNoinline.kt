import androidx.compose.runtime.*
import androidx.compose.foundation.layout.*

@Composable
fun Label(test: Boolean) {
    OuterComposableFunction {
        Column {
            if (test) return@OuterComposableFunction
        }
    }
}
