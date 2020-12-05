/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SerializationPackages {
    internal val packageFqName = FqName("kotlinx.serialization")
    internal val internalPackageFqName = FqName("kotlinx.serialization.internal")
    internal val encodingPackageFqName = FqName("kotlinx.serialization.encoding")
    internal val descriptorsPackageFqName = FqName("kotlinx.serialization.descriptors")
    internal val builtinsPackageFqName = FqName("kotlinx.serialization.builtins")

    val allPublicPackages = listOf(packageFqName, encodingPackageFqName, descriptorsPackageFqName, builtinsPackageFqName)
}

object SerializationAnnotations {
    // When changing names for these annotations, please change
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZABLE_FQ_NAME and
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZER_FQ_NAME accordingly.
    // Otherwise, there it might lead to exceptions from light classes when building them for serializer/serializable classes
    val serializableAnnotationFqName = FqName("kotlinx.serialization.Serializable")
    val serializerAnnotationFqName = FqName("kotlinx.serialization.Serializer")
    internal val serialNameAnnotationFqName = FqName("kotlinx.serialization.SerialName")
    internal val requiredAnnotationFqName = FqName("kotlinx.serialization.Required")
    val serialTransientFqName = FqName("kotlinx.serialization.Transient")
    internal val serialInfoFqName = FqName("kotlinx.serialization.SerialInfo")

    internal val contextualFqName = FqName("kotlinx.serialization.ContextualSerialization") // this one is deprecated
    internal val contextualOnFileFqName = FqName("kotlinx.serialization.UseContextualSerialization")
    internal val contextualOnPropertyFqName = FqName("kotlinx.serialization.Contextual")
    internal val polymorphicFqName = FqName("kotlinx.serialization.Polymorphic")
    internal val additionalSerializersFqName = FqName("kotlinx.serialization.UseSerializers")
}

object SerialEntityNames {
    const val KSERIALIZER_CLASS = "KSerializer"
    const val SERIAL_DESC_FIELD = "descriptor"
    const val SAVE = "serialize"
    const val LOAD = "deserialize"
    const val SERIALIZER_CLASS = "\$serializer"

    const val initializedDescriptorFieldName = "\$initializedDescriptor"

    // classes
    val KSERIALIZER_NAME = Name.identifier(KSERIALIZER_CLASS)
    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")
    val KSERIALIZER_NAME_FQ = SerializationPackages.packageFqName.child(KSERIALIZER_NAME)
    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)
    val IMPL_NAME = Name.identifier("Impl")

    val GENERATED_SERIALIZER_CLASS = Name.identifier("GeneratedSerializer")
    val GENERATED_SERIALIZER_FQ = SerializationPackages.internalPackageFqName.child(GENERATED_SERIALIZER_CLASS)

    const val ENCODER_CLASS = "Encoder"
    const val STRUCTURE_ENCODER_CLASS = "CompositeEncoder"
    const val DECODER_CLASS = "Decoder"
    const val STRUCTURE_DECODER_CLASS = "CompositeDecoder"

    const val ANNOTATION_MARKER_CLASS = "SerializableWith"

    const val SERIAL_SAVER_CLASS = "SerializationStrategy"
    const val SERIAL_LOADER_CLASS = "DeserializationStrategy"

    const val SERIAL_DESCRIPTOR_CLASS = "SerialDescriptor"
    const val SERIAL_DESCRIPTOR_CLASS_IMPL = "PluginGeneratedSerialDescriptor"
    const val SERIAL_DESCRIPTOR_FOR_ENUM = "EnumDescriptor"

    const val PLUGIN_EXCEPTIONS_FILE = "PluginExceptions"

    //exceptions
    const val SERIAL_EXC = "SerializationException"
    const val MISSING_FIELD_EXC = "MissingFieldException"
    const val UNKNOWN_FIELD_EXC = "UnknownFieldException"

    // functions
    val SERIAL_DESC_FIELD_NAME = Name.identifier(SERIAL_DESC_FIELD)
    val SAVE_NAME = Name.identifier(SAVE)
    val LOAD_NAME = Name.identifier(LOAD)
    val CHILD_SERIALIZERS_GETTER = Name.identifier("childSerializers")
    val TYPE_PARAMS_SERIALIZERS_GETTER = Name.identifier("typeParametersSerializers")
    val WRITE_SELF_NAME = Name.identifier("write\$Self")
    val SERIALIZER_PROVIDER_NAME = Name.identifier("serializer")
    val SINGLE_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwMissingFieldException")
    val ARRAY_MASK_FIELD_MISSING_FUNC_NAME = Name.identifier("throwArrayMissingFieldException")
    val SINGLE_MASK_FIELD_MISSING_FUNC_FQ = SerializationPackages.internalPackageFqName.child(SINGLE_MASK_FIELD_MISSING_FUNC_NAME)
    val ARRAY_MASK_FIELD_MISSING_FUNC_FQ = SerializationPackages.internalPackageFqName.child(ARRAY_MASK_FIELD_MISSING_FUNC_NAME)

    // parameters
    val dummyParamName = Name.identifier("serializationConstructorMarker")
    internal const val typeArgPrefix = "typeSerial"
}

object SpecialBuiltins {
    const val referenceArraySerializer = "ReferenceArraySerializer"
    const val objectSerializer = "ObjectSerializer"
    const val enumSerializer = "EnumSerializer"
    const val polymorphicSerializer = "PolymorphicSerializer"
    const val sealedSerializer = "SealedClassSerializer"
    const val contextSerializer = "ContextualSerializer"
    const val nullableSerializer = "NullableSerializer"
}

object CallingConventions {
    const val begin = "beginStructure"
    const val end = "endStructure"

    const val decode = "decode"
    const val update = "update"
    const val encode = "encode"
    const val encodeEnum = "encodeEnum"
    const val decodeEnum = "decodeEnum"
    const val decodeElementIndex = "decodeElementIndex"
    const val decodeSequentially = "decodeSequentially"
    const val elementPostfix = "Element"
    const val shouldEncodeDefault = "shouldEncodeElementDefault"

    const val addElement = "addElement"
    const val addAnnotation = "pushAnnotation"
    const val addClassAnnotation = "pushClassAnnotation"
}
