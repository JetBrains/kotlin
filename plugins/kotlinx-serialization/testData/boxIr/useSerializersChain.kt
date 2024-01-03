// WITH_STDLIB

// FILE: a.kt

package a

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

class IList<T>

abstract class DataSerializer<T, K>: KSerializer<T> {
    abstract fun getK(): K
}

class MySerializer<T>(val elementSer: KSerializer<T>): DataSerializer<IList<T>, Int>() {

    override fun getK(): Int = 42

    override val descriptor: SerialDescriptor
        get() = PrimitiveSerialDescriptor("MySer<${elementSer.descriptor.serialName}>", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: IList<T>) = TODO("serialize")

    override fun deserialize(decoder: Decoder): IList<T> = TODO("deserialize")
}

// FILE: test.kt

@file:UseSerializers(MySerializer::class)

package a

import kotlinx.serialization.*

@Serializable
class Holder(
    val i: Int,
    val c: IList<Int>
)

fun box(): String {
    val d = Holder.serializer().descriptor.toString()
    return if (d == "a.Holder(i: kotlin.Int, c: MySer<kotlin.Int>)") "OK" else d
}
