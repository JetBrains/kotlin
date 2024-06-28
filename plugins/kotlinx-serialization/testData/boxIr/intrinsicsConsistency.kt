// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import kotlinx.serialization.json.internal.*
import kotlinx.serialization.modules.*
import kotlin.reflect.*
import kotlin.test.*

class UUID

typealias Uuid = @Serializable(UuidStringSerializer::class) UUID

internal object UuidStringSerializer : KSerializer<UUID> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("UUID", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): UUID = TODO()
    override fun serialize(encoder: Encoder, value: UUID): Unit = TODO()
}


@Serializable
class Box<T>

internal class BoxSerializer<T>(val tSer: KSerializer<T>): KSerializer<Box<T>> {
    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("Box@${tSer.descriptor.serialName}", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Box<T>) = TODO()

    override fun deserialize(decoder: Decoder): Box<T> = TODO()
}

typealias BoxAlias<T> = @Serializable(BoxSerializer::class) Box<T>


@Serializable
class ByDefault

internal object NotDefaultSerializer: KSerializer<ByDefault> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("NotByDefault", PrimitiveKind.STRING)
    override fun deserialize(decoder: Decoder): ByDefault = TODO()
    override fun serialize(encoder: Encoder, value: ByDefault): Unit = TODO()
}

typealias NotDefault = @Serializable(NotDefaultSerializer::class) ByDefault

inline fun <reified T> MutableList<String>.isIntrinsicEqual(testName: String) {
    val r1 = runCatching { serializer<T>() }
    val r2 = runCatching { serializer(typeOf<T>()) }
    val areEq = (r1.isFailure && r2.isFailure) || (r1.isSuccess && r2.isSuccess)
    if (!areEq) {
        add("Results are not equal for $testName: $r1 vs $r2")
    } else if (r1.isSuccess) {
        if (r1.getOrNull()?.descriptor != r2.getOrNull()?.descriptor)
            add("Not equal descriptors: $r1 vs $r2")
    }
}

fun box(): String {
    val res = buildList {
        // Check that all variations of @Serializable do not change result of lookup
        isIntrinsicEqual<ByDefault>("ByDefault")
        isIntrinsicEqual<@Serializable(NotDefaultSerializer::class) ByDefault>("@Serializable(with) ByDefault")
        isIntrinsicEqual<NotDefault>("typealias NotDefault")
        isIntrinsicEqual<List<ByDefault>>("List ByDefault")
        isIntrinsicEqual<List<@Serializable(NotDefaultSerializer::class) ByDefault>>("List @Serializable(with) ByDefault")
        isIntrinsicEqual<List<NotDefault>>("List typealias NotDefault")

        // Same, but for generics
        isIntrinsicEqual<@Serializable(BoxSerializer::class) Box<String>>("@Serializable Box")
        isIntrinsicEqual<BoxAlias<String>>("typealias Box")
        isIntrinsicEqual<List<BoxAlias<String>>>("List typealias Box")
        isIntrinsicEqual<List<@Serializable(BoxSerializer::class) Box<String>>>("List @Serializable Box")


        // Same, but for non-serializable class — should be failures
        isIntrinsicEqual<UUID>("UUID")
        isIntrinsicEqual<@Serializable(UuidStringSerializer::class) UUID>("@Serializable(with)")
        isIntrinsicEqual<Uuid>("typealias")
        isIntrinsicEqual<List<UUID>>("List UUID")
        isIntrinsicEqual<List<@Serializable(UuidStringSerializer::class) UUID>>("List @Serializable(with)")
        isIntrinsicEqual<List<Uuid>>("List typealias")
    }
    return if (res.isEmpty()) "OK"
    else res.toString()
}
