package test

import com.jetbrains.rhizomedb.*
import kotlinx.serialization.*
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
class Point(val x: Int, val y: Int)

@Serializable(with = WrapperSerializer::class)
class Wrapper(val z: Int)

class WrapperSerializer : KSerializer<Wrapper> {
    override val descriptor: SerialDescriptor
        get() = Int.serializer().descriptor

    override fun deserialize(decoder: Decoder): Wrapper {
        return Wrapper(decoder.decodeInt())
    }

    override fun serialize(encoder: Encoder, value: Wrapper) {
        encoder.encodeInt(value.z)
    }
}

class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val str: String by strAttr

    @ValueAttribute
    val int: Int by intAttr

    @ValueAttribute
    val set: Set<String> by setAttr

    @ValueAttribute
    val serializableClass: Point by serializableClassAttr

    @ValueAttribute
    val serializableClassList: List<Point> by serializableClassListAttr

    @ValueAttribute
    val manuallySerializableClass: Wrapper by manuallySerializableClassAttr

    @ValueAttribute
    val manuallySerializableClassMap: Map<String, Wrapper> by manuallySerializableClassMapAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
