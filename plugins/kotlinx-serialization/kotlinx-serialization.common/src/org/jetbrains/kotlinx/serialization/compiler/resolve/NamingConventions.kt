/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.resolve

import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

object SerializationPackages {
    val packageFqName = FqName("kotlinx.serialization")
    val internalPackageFqName = FqName("kotlinx.serialization.internal")
    val encodingPackageFqName = FqName("kotlinx.serialization.encoding")
    val descriptorsPackageFqName = FqName("kotlinx.serialization.descriptors")
    val builtinsPackageFqName = FqName("kotlinx.serialization.builtins")

    val allPublicPackages = listOf(packageFqName, encodingPackageFqName, descriptorsPackageFqName, builtinsPackageFqName)
}

object SerializationAnnotations {
    // When changing names for these annotations, please change
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZABLE_FQ_NAME and
    // org.jetbrains.kotlin.idea.caches.lightClasses.annotations.KOTLINX_SERIALIZER_FQ_NAME accordingly.
    // Otherwise, there it might lead to exceptions from light classes when building them for serializer/serializable classes
    val serializableAnnotationFqName = FqName("kotlinx.serialization.Serializable")
    val serializerAnnotationFqName = FqName("kotlinx.serialization.Serializer")
    val serialNameAnnotationFqName = FqName("kotlinx.serialization.SerialName")
    val requiredAnnotationFqName = FqName("kotlinx.serialization.Required")
    val serialTransientFqName = FqName("kotlinx.serialization.Transient")

    // Also implicitly used in kotlin-native.compiler.backend.native/CodeGenerationInfo.kt
    val serialInfoFqName = FqName("kotlinx.serialization.SerialInfo")
    val inheritableSerialInfoFqName = FqName("kotlinx.serialization.InheritableSerialInfo")
    val metaSerializableAnnotationFqName = FqName("kotlinx.serialization.MetaSerializable")
    val encodeDefaultFqName = FqName("kotlinx.serialization.EncodeDefault")

    val contextualFqName = FqName("kotlinx.serialization.ContextualSerialization") // this one is deprecated
    val contextualOnFileFqName = FqName("kotlinx.serialization.UseContextualSerialization")
    val contextualOnPropertyFqName = FqName("kotlinx.serialization.Contextual")
    val polymorphicFqName = FqName("kotlinx.serialization.Polymorphic")
    val additionalSerializersFqName = FqName("kotlinx.serialization.UseSerializers")

    val namedCompanionFqName = FqName("kotlinx.serialization.internal.NamedCompanion")

    val serializableAnnotationClassId = ClassId.topLevel(serializableAnnotationFqName)
    val serializerAnnotationClassId = ClassId.topLevel(serializerAnnotationFqName)
    val serialNameAnnotationClassId = ClassId.topLevel(serialNameAnnotationFqName)
    val requiredAnnotationClassId = ClassId.topLevel(requiredAnnotationFqName)
    val serialTransientClassId = ClassId.topLevel(serialTransientFqName)
    val serialInfoClassId = ClassId.topLevel(serialInfoFqName)
    val inheritableSerialInfoClassId = ClassId.topLevel(inheritableSerialInfoFqName)
    val metaSerializableAnnotationClassId = ClassId.topLevel(metaSerializableAnnotationFqName)
    val encodeDefaultClassId = ClassId.topLevel(encodeDefaultFqName)

    val contextualClassId = ClassId.topLevel(contextualFqName)
    val contextualOnFileClassId = ClassId.topLevel(contextualOnFileFqName)
    val contextualOnPropertyClassId = ClassId.topLevel(contextualOnPropertyFqName)
    val polymorphicClassId = ClassId.topLevel(polymorphicFqName)
    val additionalSerializersClassId = ClassId.topLevel(additionalSerializersFqName)
    val namedCompanionClassId = ClassId.topLevel(namedCompanionFqName)
}

object SerialEntityNames {
    const val KSERIALIZER_CLASS = "KSerializer"
    const val SERIAL_DESC_FIELD = "descriptor"
    const val SAVE = "serialize"
    const val LOAD = "deserialize"
    const val SERIALIZER_CLASS = "\$serializer"

    const val CACHED_DESCRIPTOR_FIELD = "\$cachedDescriptor"
    const val CACHED_SERIALIZER_PROPERTY = "\$cachedSerializer"
    const val CACHED_CHILD_SERIALIZERS_PROPERTY = "\$childSerializers"

    // classes
    val KCLASS_NAME_FQ = FqName("kotlin.reflect.KClass")
    val KCLASS_NAME_CLASS_ID = ClassId.topLevel(KCLASS_NAME_FQ)
    val KSERIALIZER_NAME = Name.identifier(KSERIALIZER_CLASS)
    val SERIAL_CTOR_MARKER_NAME = Name.identifier("SerializationConstructorMarker")
    val KSERIALIZER_NAME_FQ = SerializationPackages.packageFqName.child(KSERIALIZER_NAME)
    val KSERIALIZER_CLASS_ID = ClassId.topLevel(KSERIALIZER_NAME_FQ)

    val SERIALIZER_CLASS_NAME = Name.identifier(SERIALIZER_CLASS)
    val IMPL_NAME = Name.identifier("Impl")

    val GENERATED_SERIALIZER_CLASS = Name.identifier("GeneratedSerializer")
    val GENERATED_SERIALIZER_FQ = SerializationPackages.internalPackageFqName.child(GENERATED_SERIALIZER_CLASS)

    val SERIALIZER_FACTORY_INTERFACE_NAME = Name.identifier("SerializerFactory")

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
    const val SERIAL_DESCRIPTOR_FOR_INLINE = "InlineClassDescriptor"

    const val PLUGIN_EXCEPTIONS_FILE = "PluginExceptions"
    const val ENUMS_FILE = "Enums"

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
    val ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createSimpleEnumSerializer")
    val ANNOTATED_ENUM_SERIALIZER_FACTORY_FUNC_NAME = Name.identifier("createAnnotatedEnumSerializer")
    val SINGLE_MASK_FIELD_MISSING_FUNC_FQ = SerializationPackages.internalPackageFqName.child(SINGLE_MASK_FIELD_MISSING_FUNC_NAME)
    val ARRAY_MASK_FIELD_MISSING_FUNC_FQ = SerializationPackages.internalPackageFqName.child(ARRAY_MASK_FIELD_MISSING_FUNC_NAME)
    val CACHED_SERIALIZER_PROPERTY_NAME = Name.identifier(CACHED_SERIALIZER_PROPERTY)
    val CACHED_CHILD_SERIALIZERS_PROPERTY_NAME = Name.identifier(CACHED_CHILD_SERIALIZERS_PROPERTY)
    val CACHED_DESCRIPTOR_FIELD_NAME = Name.identifier(CACHED_DESCRIPTOR_FIELD)

    // parameters
    val dummyParamName = Name.identifier("serializationConstructorMarker")
    const val typeArgPrefix = "typeSerial"

    val wrapIntoNullableExt = SerializationPackages.builtinsPackageFqName.child(Name.identifier("nullable"))
    val wrapIntoNullableCallableId = CallableId(SerializationPackages.builtinsPackageFqName, Name.identifier("nullable"))
}

object SpecialBuiltins {
    const val referenceArraySerializer = "ReferenceArraySerializer"
    const val objectSerializer = "ObjectSerializer"
    const val enumSerializer = "EnumSerializer"
    const val polymorphicSerializer = "PolymorphicSerializer"
    const val sealedSerializer = "SealedClassSerializer"
    const val contextSerializer = "ContextualSerializer"
    const val nullableSerializer = "NullableSerializer"

    object Names {
        val referenceArraySerializer = Name.identifier(SpecialBuiltins.referenceArraySerializer)
        val objectSerializer = Name.identifier(SpecialBuiltins.objectSerializer)
        val enumSerializer = Name.identifier(SpecialBuiltins.enumSerializer)
        val polymorphicSerializer = Name.identifier(SpecialBuiltins.polymorphicSerializer)
        val sealedSerializer = Name.identifier(SpecialBuiltins.sealedSerializer)
        val contextSerializer = Name.identifier(SpecialBuiltins.contextSerializer)
        val nullableSerializer = Name.identifier(SpecialBuiltins.nullableSerializer)
    }
}

object PrimitiveBuiltins {
    const val booleanSerializer = "BooleanSerializer"
    const val byteSerializer = "ByteSerializer"
    const val shortSerializer = "ShortSerializer"
    const val intSerializer = "IntSerializer"
    const val longSerializer = "LongSerializer"
    const val floatSerializer = "FloatSerializer"
    const val doubleSerializer = "DoubleSerializer"
    const val charSerializer = "CharSerializer"
}


object CallingConventions {
    const val begin = "beginStructure"
    const val end = "endStructure"

    const val decode = "decode"
    const val update = "update"
    const val encode = "encode"
    const val encodeEnum = "encodeEnum"
    const val decodeEnum = "decodeEnum"
    const val encodeInline = "encodeInline"
    const val decodeInline = "decodeInline"
    const val decodeElementIndex = "decodeElementIndex"
    const val decodeSequentially = "decodeSequentially"
    const val elementPostfix = "Element"
    const val shouldEncodeDefault = "shouldEncodeElementDefault"

    const val addElement = "addElement"
    const val addAnnotation = "pushAnnotation"
    const val addClassAnnotation = "pushClassAnnotation"
}

object SerializationDependencies {
    val LAZY_FQ = FqName("kotlin.Lazy")
    val LAZY_FUNC_FQ = FqName("kotlin.lazy")
    val LAZY_MODE_FQ = FqName("kotlin.LazyThreadSafetyMode")
    val FUNCTION0_FQ = FqName("kotlin.Function0")
    val LAZY_PUBLICATION_MODE_NAME = Name.identifier("PUBLICATION")
}

object SerializationJsDependenciesClassIds {
    val jsExportIgnore = ClassId.fromString("kotlin/js/JsExport.Ignore")
}

object SerializersClassIds {
    val kSerializerId = ClassId(SerializationPackages.packageFqName, SerialEntityNames.KSERIALIZER_NAME)
    val enumSerializerId = ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.enumSerializer))
    val polymorphicSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.polymorphicSerializer))
    val referenceArraySerializerId =
        ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.referenceArraySerializer))
    val objectSerializerId = ClassId(SerializationPackages.internalPackageFqName, Name.identifier(SpecialBuiltins.objectSerializer))
    val sealedSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.sealedSerializer))
    val contextSerializerId = ClassId(SerializationPackages.packageFqName, Name.identifier(SpecialBuiltins.contextSerializer))
    val generatedSerializerId = ClassId(SerializationPackages.internalPackageFqName, SerialEntityNames.GENERATED_SERIALIZER_CLASS)

    val setOfSpecialSerializers = setOf(contextSerializerId, polymorphicSerializerId)
}

object SerializationRuntimeClassIds {

    val descriptorClassId =
        ClassId(SerializationPackages.descriptorsPackageFqName, Name.identifier(SerialEntityNames.SERIAL_DESCRIPTOR_CLASS))
    val compositeEncoderClassId =
        ClassId(SerializationPackages.encodingPackageFqName, Name.identifier(SerialEntityNames.STRUCTURE_ENCODER_CLASS))
}

fun findStandardKotlinTypeSerializerName(typeName: String?): String? {
    return when (typeName) {
        null -> null
        "kotlin.Unit" -> "UnitSerializer"
        "kotlin.Nothing" -> "NothingSerializer"
        "kotlin.Boolean" -> "BooleanSerializer"
        "kotlin.Byte" -> "ByteSerializer"
        "kotlin.Short" -> "ShortSerializer"
        "kotlin.Int" -> "IntSerializer"
        "kotlin.Long" -> "LongSerializer"
        "kotlin.Float" -> "FloatSerializer"
        "kotlin.Double" -> "DoubleSerializer"
        "kotlin.Char" -> "CharSerializer"
        "kotlin.UInt" -> "UIntSerializer"
        "kotlin.ULong" -> "ULongSerializer"
        "kotlin.UByte" -> "UByteSerializer"
        "kotlin.UShort" -> "UShortSerializer"
        "kotlin.String" -> "StringSerializer"
        "kotlin.Pair" -> "PairSerializer"
        "kotlin.Triple" -> "TripleSerializer"
        "kotlin.collections.Collection", "kotlin.collections.List",
        "kotlin.collections.ArrayList", "kotlin.collections.MutableList" -> "ArrayListSerializer"
        "kotlin.collections.Set", "kotlin.collections.LinkedHashSet", "kotlin.collections.MutableSet" -> "LinkedHashSetSerializer"
        "kotlin.collections.HashSet" -> "HashSetSerializer"
        "kotlin.collections.Map", "kotlin.collections.LinkedHashMap", "kotlin.collections.MutableMap" -> "LinkedHashMapSerializer"
        "kotlin.collections.HashMap" -> "HashMapSerializer"
        "kotlin.collections.Map.Entry" -> "MapEntrySerializer"
        "kotlin.ByteArray" -> "ByteArraySerializer"
        "kotlin.ShortArray" -> "ShortArraySerializer"
        "kotlin.IntArray" -> "IntArraySerializer"
        "kotlin.LongArray" -> "LongArraySerializer"
        "kotlin.UByteArray" -> "UByteArraySerializer"
        "kotlin.UShortArray" -> "UShortArraySerializer"
        "kotlin.UIntArray" -> "UIntArraySerializer"
        "kotlin.ULongArray" -> "ULongArraySerializer"
        "kotlin.CharArray" -> "CharArraySerializer"
        "kotlin.FloatArray" -> "FloatArraySerializer"
        "kotlin.DoubleArray" -> "DoubleArraySerializer"
        "kotlin.BooleanArray" -> "BooleanArraySerializer"
        "kotlin.time.Duration" -> "DurationSerializer"
        "java.lang.Boolean" -> "BooleanSerializer"
        "java.lang.Byte" -> "ByteSerializer"
        "java.lang.Short" -> "ShortSerializer"
        "java.lang.Integer" -> "IntSerializer"
        "java.lang.Long" -> "LongSerializer"
        "java.lang.Float" -> "FloatSerializer"
        "java.lang.Double" -> "DoubleSerializer"
        "java.lang.Character" -> "CharSerializer"
        "java.lang.String" -> "StringSerializer"
        "java.util.Collection", "java.util.List", "java.util.ArrayList" -> "ArrayListSerializer"
        "java.util.Set", "java.util.LinkedHashSet" -> "LinkedHashSetSerializer"
        "java.util.HashSet" -> "HashSetSerializer"
        "java.util.Map", "java.util.LinkedHashMap" -> "LinkedHashMapSerializer"
        "java.util.HashMap" -> "HashMapSerializer"
        "java.util.Map.Entry" -> "MapEntrySerializer"
        else -> return null
    }
}
