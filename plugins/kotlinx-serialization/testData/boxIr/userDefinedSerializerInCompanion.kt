// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.builtins.*


@Serializable
data class Main(val fields: MainFields) {
    companion object  {
        fun fieldsSerializer(): KSerializer<MainFields> = MainFields.serializer()
    }
}

@Serializable
data class MainFields(val firstName: String?)

@Serializable
data class Box<T>(val boxed: T) {
    companion object {
        fun <T> serializerLike(tSer: KSerializer<T>): KSerializer<List<Box<T>>> = ListSerializer(serializer(tSer))
    }
}


fun box(): String {
    if (Main.fieldsSerializer().descriptor.toString() != "MainFields(firstName: kotlin.String?)") return "Error1"
    if (MainFields.serializer().descriptor.toString() != "MainFields(firstName: kotlin.String?)") return "Error2"
    if (Main.serializer().descriptor.toString() != "Main(fields: MainFields)") return "Error3"
    val boxListDesc = Box.serializerLike(String.serializer()).descriptor
    if (boxListDesc.toString() != "kotlin.collections.ArrayList(Box(boxed: kotlin.String))") return boxListDesc.toString()
    val boxDesc = Box.serializer(String.serializer()).descriptor
    if (boxDesc.toString() != "Box(boxed: kotlin.String)") return boxDesc.toString()
    return "OK"
}
