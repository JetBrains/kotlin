// COMPILER_PLUGIN: kotlin-serialization-compiler-plugin-2.4.20.jar disableIntrinsic=false

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable

@Serializable
data class Message(val id: Int, val text: String)

fun box(): String {
    val serializer: KSerializer<Message> = Message.serializer()
    return "OK"
}
