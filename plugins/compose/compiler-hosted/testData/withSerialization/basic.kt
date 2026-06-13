import androidx.compose.runtime.Composable
import androidx.compose.foundation.text.BasicText
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*

@Serializable
data class Data(val ok: String) {
    @Transient
    val trans = "ok"
}

@Composable
fun ClickCounter(clicks: Int) {
    BasicText("Test $clicks")
}

fun box(): String {
    val data = Data("ok")
    val encoded = Json.encodeToString(data)
    val decoded = Json.decodeFromString<Data>(encoded)
    if (decoded.ok != "ok") return "FAIL instance"
    if (decoded.trans != "ok") return "FAIL transient"
    if (Data.serializer().descriptor.elementNames.single() != "ok") return "FAIL descriptor"
    return "OK"
}
