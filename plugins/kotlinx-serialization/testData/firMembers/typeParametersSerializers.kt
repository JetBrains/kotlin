// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.internal.*

@Serializable
class NoTypeParams(val value: String)

@Serializable
class WithTypeParams<T>(val value: T)

@OptIn(InternalSerializationApi::class)
fun box(): String {
    val noTypeParamsSerializer = NoTypeParams.serializer() as GeneratedSerializer<*>
    val typeParamsEmpty = noTypeParamsSerializer.typeParametersSerializers()
    if (typeParamsEmpty.size != 0) return "Expected empty array for NoTypeParams, got size ${typeParamsEmpty.size}"

    val withTypeParamsSerializer = WithTypeParams.serializer(String.serializer()) as GeneratedSerializer<*>
    val typeParamsOne = withTypeParamsSerializer.typeParametersSerializers()
    if (typeParamsOne.size != 1) return "Expected array of size 1 for WithTypeParams, got size ${typeParamsOne.size}"

    return "OK"
}
