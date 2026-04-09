import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.BasicText
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*

@Serializable
data class Data(val ok: String)

@Composable
fun ClickCounter(clicks: Int) {
    BasicText("Test $clicks")
}

fun box(): String {
    return Data.serializer().descriptor.elementNames.single().uppercase()
}
