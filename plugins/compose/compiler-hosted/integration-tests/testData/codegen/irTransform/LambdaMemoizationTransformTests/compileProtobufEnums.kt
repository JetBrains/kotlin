import androidx.compose.runtime.*
import androidx.compose.compiler.plugins.EnumTestProtos

@Composable
fun Test(parameter: EnumTestProtos.Enum) {
    val lambda = { println(parameter) }
}
