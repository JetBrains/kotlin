import androidx.compose.runtime.Composable

var effects = mutableListOf<Any>()
var outside: Any = ""
var number = 1

@Composable fun Wrap(block: @Composable () -> Unit) =  block()
@Composable fun <T> effect(block: () -> T): T = block()
