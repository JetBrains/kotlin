// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.modules.*
import kotlinx.serialization.encoding.*

class SomeData<T>(val t: T)

@Serializable
class PagedData<T>(
    @Contextual val someData: SomeData<T>,
)

class SomeDataSerializer<T>(val tSer: KSerializer<T>) : KSerializer<SomeData<T>> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SomeData")

    override fun serialize(encoder: Encoder, value: SomeData<T>) {
        encoder as JsonEncoder
        val data = encoder.json.encodeToJsonElement(tSer, value.t)
        val obj = buildJsonObject {
            put("innerType", tSer.descriptor.serialName)
            put("data", data)
        }
        encoder.encodeJsonElement(obj)
    }

    override fun deserialize(decoder: Decoder): SomeData<T> {
        TODO("Not yet implemented")
    }
}

fun box(): String {
    val module = SerializersModule {
        contextual(SomeData::class) { args -> SomeDataSerializer(args[0]) }
    }
    val json = Json { serializersModule = module }
    val input = PagedData<String>(SomeData("foo_bar"))
    val enc = json.encodeToString(input)
    return if (enc != """{"someData":{"innerType":"kotlin.String","data":"foo_bar"}}""") enc else "OK"
}
