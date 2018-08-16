package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SerializationPackages {
    internal val packageFqName = FqName("kotlinx.serialization")
    internal val internalPackageFqName = FqName("kotlinx.serialization.internal")
    internal val builtinsPackageFqName = FqName("kotlinx.serialization.builtins")
}

object SerializationAnnotations {
    internal val serializableAnnotationFqName = FqName("kotlinx.serialization.Serializable")
    internal val serializerAnnotationFqName = FqName("kotlinx.serialization.Serializer")
    internal val serialNameAnnotationFqName = FqName("kotlinx.serialization.SerialName")
    internal val serialOptionalFqName = FqName("kotlinx.serialization.Optional")
    internal val serialTransientFqName = FqName("kotlinx.serialization.Transient")
    internal val serialInfoFqName = FqName("kotlinx.serialization.SerialInfo")
}

object SerialEntityNames {
    const val KSERIALIZER_CLASS = "KSerializer"
    const val SERIAL_DESC_FIELD = "descriptor"
    const val SAVE = "serialize"
    const val LOAD = "deserialize"
    const val SERIALIZER_CLASS = "\$serializer"

    // classes
    val KSERIALIZER_NAME = Name.identifier(KSERIALIZER_CLASS)
    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")
    val KSERIALIZER_NAME_FQ = SerializationPackages.packageFqName.child(KSERIALIZER_NAME)
    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)
    val IMPL_NAME = Name.identifier("Impl")

    const val ENCODER_CLASS = "Encoder"
    const val STRUCTURE_ENCODER_CLASS = "CompositeEncoder"
    const val DECODER_CLASS = "Decoder"
    const val STRUCTURE_DECODER_CLASS = "CompositeDecoder"

    const val SERIAL_SAVER_CLASS = "SerializationStrategy"
    const val SERIAL_LOADER_CLASS = "DeserializationStrategy"

    const val SERIAL_DESCRIPTOR_CLASS = "SerialDescriptor"
    const val SERIAL_DESCRIPTOR_CLASS_IMPL = "SerialClassDescImpl"

    //exceptions
    const val SERIAL_EXC = "SerializationException"
    const val MISSING_FIELD_EXC = "MissingFieldException"
    const val UNKNOWN_FIELD_EXC = "UnknownFieldException"

    // functions
    val SERIAL_DESC_FIELD_NAME = Name.identifier(SERIAL_DESC_FIELD)
    val SAVE_NAME = Name.identifier(SAVE)
    val LOAD_NAME = Name.identifier(LOAD)
    val WRITE_SELF_NAME = Name.identifier("write\$Self")
    val SERIALIZER_PROVIDER_NAME = Name.identifier("serializer")

    // parameters
    val dummyParamName = Name.identifier("serializationConstructorMarker")
    internal const val typeArgPrefix = "typeSerial"
}

object SpecialBuiltins {
    const val referenceArraySerializer = "ReferenceArraySerializer"
    const val enumSerializer = "EnumSerializer"
    const val polymorphicSerializer = "PolymorphicSerializer"
    const val contextSerializer = "ContextSerializer"
    const val nullableSerializer = "NullableSerializer"
}

object CallingConventions {
    const val begin = "beginStructure"
    const val end = "endStructure"

    const val decode = "decode"
    const val update = "update"
    const val encode = "encode"
    const val decodeElementIndex = "decodeElementIndex"
    const val elementPostfix = "Element"

    const val addElement = "addElement"
    const val addAnnotation = "pushAnnotation"
    const val addClassAnnotation = "pushClassAnnotation"
}