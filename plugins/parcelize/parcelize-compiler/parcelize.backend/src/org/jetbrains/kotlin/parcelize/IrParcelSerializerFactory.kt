/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize

import org.jetbrains.kotlin.backend.jvm.ir.erasedUpperBound
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.inlineClassRepresentation
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.parcelize.ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES

class IrParcelSerializerFactory(private val symbols: AndroidSymbols) {
    private val supportedBySimpleListSerializer = setOf(
        "kotlin.collections.List", "kotlin.collections.MutableList", "kotlin.collections.ArrayList",
        "java.util.List", "java.util.ArrayList",
        *BuiltinParcelableTypes.IMMUTABLE_LIST_FQNAMES.toTypedArray()
    )

    // TODO: More java collections?
    // TODO: Add tests for all of these types, not just some common ones...
    private val supportedByListSerializer = setOf(
        "kotlin.collections.MutableList", "kotlin.collections.List", "java.util.List",
        "kotlin.collections.ArrayList", "java.util.ArrayList",
        "kotlin.collections.ArrayDeque", "java.util.ArrayDeque",
        "kotlin.collections.MutableSet", "kotlin.collections.Set", "java.util.Set",
        "kotlin.collections.HashSet", "java.util.HashSet",
        "kotlin.collections.LinkedHashSet", "java.util.LinkedHashSet",
        "java.util.NavigableSet", "java.util.SortedSet",
        *BuiltinParcelableTypes.IMMUTABLE_LIST_FQNAMES.toTypedArray(),
        *BuiltinParcelableTypes.IMMUTABLE_SET_FQNAMES.toTypedArray(),
    )

    private val supportedByMapSerializer = setOf(
        "kotlin.collections.MutableMap", "kotlin.collections.Map", "java.util.Map",
        "kotlin.collections.HashMap", "java.util.HashMap",
        "kotlin.collections.LinkedHashMap", "java.util.LinkedHashMap",
        "java.util.SortedMap", "java.util.NavigableMap", "java.util.TreeMap",
        "java.util.concurrent.ConcurrentHashMap",
        *BuiltinParcelableTypes.IMMUTABLE_MAP_FQNAMES.toTypedArray(),
    )

    /**
     * Resolve the given [irType] to a corresponding [IrParcelSerializer]. This depends on the TypeParcelers which
     * are currently in [scope], as well as the type of the enclosing Parceleable class [parcelizeType], which is needed
     * to get a class loader for reflection based serialization. Beyond this, we need to know whether to allow
     * using read/writeValue for serialization (if [strict] is false). Beyond this, we need to know whether we are
     * producing parcelers for properties of a Parcelable (if [toplevel] is true), or for a complete Parcelable.
     */
    fun get(
        irType: IrType,
        scope: IrParcelerScope?,
        parcelizeType: IrType,
        strict: Boolean = false,
        toplevel: Boolean = false
    ): IrParcelSerializer {
        fun strict() = strict && !irType.hasAnyAnnotation(RAW_VALUE_ANNOTATION_FQ_NAMES)

        scope.getCustomSerializer(irType)?.let { parceler ->
            return IrCustomParcelSerializer(parceler)
        }

        val classifier = irType.erasedUpperBound
        val classifierFqName = classifier.fqNameWhenAvailable?.asString()
        when (classifierFqName) {
            // Built-in parcel serializers
            "kotlin.String", "java.lang.String" ->
                return stringSerializer
            "kotlin.CharSequence", "java.lang.CharSequence" ->
                return charSequenceSerializer
            "android.os.Bundle" ->
                return IrParcelSerializerWithClassLoader(parcelizeType, symbols.parcelReadBundle, symbols.parcelWriteBundle)
            "android.os.PersistableBundle" ->
                return IrParcelSerializerWithClassLoader(
                    parcelizeType,
                    symbols.parcelReadPersistableBundle,
                    symbols.parcelWritePersistableBundle
                )

            // Non-nullable built-in serializers
            "kotlin.Byte", "java.lang.Byte" ->
                return wrapNullableSerializerIfNeeded(irType, byteSerializer)
            "kotlin.Boolean", "java.lang.Boolean" ->
                return wrapNullableSerializerIfNeeded(irType, booleanSerializer)
            "kotlin.Char", "java.lang.Character" ->
                return wrapNullableSerializerIfNeeded(irType, charSerializer)
            "kotlin.Short", "java.lang.Short" ->
                return wrapNullableSerializerIfNeeded(irType, shortSerializer)
            "kotlin.Int", "java.lang.Integer" ->
                return wrapNullableSerializerIfNeeded(irType, intSerializer)
            "kotlin.Long", "java.lang.Long" ->
                return wrapNullableSerializerIfNeeded(irType, longSerializer)
            "kotlin.Float", "java.lang.Float" ->
                return wrapNullableSerializerIfNeeded(irType, floatSerializer)
            "kotlin.Double", "java.lang.Double" ->
                return wrapNullableSerializerIfNeeded(irType, doubleSerializer)
            "java.io.FileDescriptor" ->
                return wrapNullableSerializerIfNeeded(irType, fileDescriptorSerializer)
            "android.util.Size" ->
                return wrapNullableSerializerIfNeeded(irType, sizeSerializer)
            "android.util.SizeF" ->
                return wrapNullableSerializerIfNeeded(irType, sizeFSerializer)

            // Unsigned primitive types
            "kotlin.UByte" ->
                return wrapNullableSerializerIfNeeded(irType, ubyteSerializer)
            "kotlin.UShort" ->
                return wrapNullableSerializerIfNeeded(irType, ushortSerializer)
            "kotlin.UInt" ->
                return wrapNullableSerializerIfNeeded(irType, uintSerializer)
            "kotlin.ULong" ->
                return wrapNullableSerializerIfNeeded(irType, ulongSerializer)

            // Built-in non-parameterized container types.
            "kotlin.IntArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.intType))
                    return intArraySerializer
            "kotlin.BooleanArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.booleanType))
                    return booleanArraySerializer
            "kotlin.ByteArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.byteType))
                    return byteArraySerializer
            "kotlin.CharArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.charType))
                    return charArraySerializer
            "kotlin.FloatArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.floatType))
                    return floatArraySerializer
            "kotlin.DoubleArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.doubleType))
                    return doubleArraySerializer
            "kotlin.LongArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.longType))
                    return longArraySerializer
            "android.util.SparseBooleanArray" ->
                if (!scope.hasCustomSerializer(irBuiltIns.booleanType))
                    return sparseBooleanArraySerializer

            // Unsigned array types
            "kotlin.UByteArray" ->
                return ubyteArraySerializer
            "kotlin.UShortArray" ->
                return wrapNullableSerializerIfNeeded(
                    irType,
                    ushortArraySerializer
                )
            "kotlin.UIntArray" ->
                return uintArraySerializer
            "kotlin.ULongArray" ->
                return ulongArraySerializer
            // Library types
            "kotlin.time.Duration" ->
                return wrapNullableSerializerIfNeeded(irType, durationSerializer)
        }

        // Generic container types
        when (classifierFqName) {
            // Apart from kotlin.Array and kotlin.ShortArray, these will only be hit if we have a
            // special parceler for the element type.
            "kotlin.Array", "kotlin.ShortArray", "kotlin.IntArray",
            "kotlin.BooleanArray", "kotlin.ByteArray", "kotlin.CharArray",
            "kotlin.FloatArray", "kotlin.DoubleArray", "kotlin.LongArray" -> {
                val elementType = irType.getArrayElementType(irBuiltIns)

                if (!scope.hasCustomSerializer(elementType)) {
                    when (elementType.erasedUpperBound.fqNameWhenAvailable?.asString()) {
                        "java.lang.String", "kotlin.String" ->
                            return stringArraySerializer
                        "android.os.IBinder" ->
                            return iBinderArraySerializer
                    }
                }

                val arrayType =
                    if (classifier.defaultType.isPrimitiveArray()) classifier.defaultType else irBuiltIns.arrayClass.typeWith(elementType)
                return wrapNullableSerializerIfNeeded(
                    irType,
                    IrArrayParcelSerializer(arrayType, elementType, get(elementType, scope, parcelizeType, strict()))
                )
            }

            // This will only be hit if we have a custom serializer for booleans
            "android.util.SparseBooleanArray" ->
                return wrapNullableSerializerIfNeeded(
                    irType,
                    IrSparseArrayParcelSerializer(
                        classifier,
                        irBuiltIns.booleanType,
                        get(irBuiltIns.booleanType, scope, parcelizeType, strict())
                    )
                )
            "android.util.SparseIntArray" ->
                return wrapNullableSerializerIfNeeded(
                    irType,
                    IrSparseArrayParcelSerializer(
                        classifier,
                        irBuiltIns.intType,
                        get(irBuiltIns.intType, scope, parcelizeType, strict())
                    )
                )
            "android.util.SparseLongArray" ->
                return wrapNullableSerializerIfNeeded(
                    irType,
                    IrSparseArrayParcelSerializer(
                        classifier,
                        irBuiltIns.longType,
                        get(irBuiltIns.longType, scope, parcelizeType, strict())
                    )
                )
            "android.util.SparseArray" -> {
                val elementType = (irType as IrSimpleType).arguments.single().upperBound(irBuiltIns)
                return wrapNullableSerializerIfNeeded(
                    irType,
                    IrSparseArrayParcelSerializer(
                        classifier,
                        elementType,
                        get(elementType, scope, parcelizeType, strict())
                    )
                )
            }

            in supportedByListSerializer -> {
                val elementType = (irType as IrSimpleType).arguments.single().upperBound(irBuiltIns)
                if (!scope.hasCustomSerializer(elementType) &&
                    classifierFqName in supportedBySimpleListSerializer
                ) {
                    val elementTypeAsString = elementType.erasedUpperBound.fqNameWhenAvailable?.asString()
                    val simpleSerializer =
                        if (classifierFqName in BuiltinParcelableTypes.IMMUTABLE_LIST_FQNAMES) {
                            when (elementTypeAsString) {
                                "android.os.IBinder" -> iBinderPersistentListSerializer
                                "kotlin.String", "java.lang.String" -> stringPersistentListSerializer
                                else -> null
                            }
                        } else {
                            when (elementTypeAsString) {
                                "android.os.IBinder" -> iBinderListSerializer
                                "kotlin.String", "java.lang.String" -> stringListSerializer
                                else -> null
                            }
                        }

                    if (simpleSerializer != null) {
                        return simpleSerializer
                    }
                }

                val listSerializer = IrListParcelSerializer(classifier, elementType, get(elementType, scope, parcelizeType, strict()))
                val actualSerializer =
                    when (classifierFqName) {
                        in BuiltinParcelableTypes.IMMUTABLE_LIST_FQNAMES -> IrExtensionFunctionOnReadCallingSerializer(
                            delegated = listSerializer,
                            converterExtensionFunction = symbols.kotlinIterableToPersistentListExtension
                        )
                        in BuiltinParcelableTypes.IMMUTABLE_SET_FQNAMES -> IrExtensionFunctionOnReadCallingSerializer(
                            delegated = listSerializer,
                            converterExtensionFunction = symbols.kotlinIterableToPersistentSetExtension
                        )
                        else -> listSerializer
                    }

                return wrapNullableSerializerIfNeeded(
                    irType,
                    actualSerializer
                )
            }

            in supportedByMapSerializer -> {
                val keyType = (irType as IrSimpleType).arguments[0].upperBound(irBuiltIns)
                val valueType = irType.arguments[1].upperBound(irBuiltIns)
                val mapSerializer =
                    IrMapParcelSerializer(
                        classifier,
                        keyType,
                        valueType,
                        get(keyType, scope, parcelizeType, strict()),
                        get(valueType, scope, parcelizeType, strict())
                    )

                val actualSerializer =
                    if (classifierFqName in BuiltinParcelableTypes.IMMUTABLE_MAP_FQNAMES) {
                        IrExtensionFunctionOnReadCallingSerializer(
                            mapSerializer,
                            symbols.kotlinMapToPersistentMapExtension
                        )
                    } else {
                        mapSerializer
                    }
                return wrapNullableSerializerIfNeeded(irType, actualSerializer)
            }
        }

        // Generic parcelable types
        when {
            classifier.isSubclassOfFqName("android.os.Parcelable")
                    // Avoid infinite loops when deriving parcelers for enum or object classes.
                    && !(toplevel && (classifier.isObject || classifier.isEnumClass)) -> {
                // We try to use writeToParcel/createFromParcel directly whenever possible, but there are some caveats.
                //
                // According to the JLS, changing a class from final to non-final is a binary compatible change, hence we
                // cannot use the writeToParcel/createFromParcel methods directly when serializing classes from external
                // dependencies. This issue was originally reported in KT-20029.
                //
                // Conversely, we can and should apply this optimization to all classes in the current module which
                // implement Parcelable (KT-20030). There are two cases to consider. If the class is annotated with
                // @Parcelize, we will create the corresponding methods/fields ourselves, before we generate the code
                // for writeToParcel/createFromParcel. For Java classes (or compiled Kotlin classes annotated with
                // @Parcelize), we'll have a field in the class itself. Finally, with Parcelable instances which were
                // manually implemented in Kotlin, we'll instead have an @JvmField property getter in the companion object.
                return if (classifier.modality == Modality.FINAL && classifier.psiElement != null
                    && (classifier.isParcelize || classifier.hasCreatorField)
                ) {
                    wrapNullableSerializerIfNeeded(irType, IrEfficientParcelableParcelSerializer(classifier))
                } else {
                    // In all other cases, we have to use the generic methods in Parcel, which use reflection internally.
                    IrGenericParcelableParcelSerializer(parcelizeType)
                }
            }

            classifier.isSubclassOfFqName("android.os.IBinder") ->
                return iBinderSerializer

            classifier.isObject ->
                return IrObjectParcelSerializer(classifier)

            classifier.isEnumClass ->
                return wrapNullableSerializerIfNeeded(irType, IrEnumParcelSerializer(classifier))

            classifier.isSubclassOfFqName("java.io.Serializable")
                    // Functions and Continuations are always serializable.
                    || irType.isFunctionTypeOrSubtype() || irType.isSuspendFunctionTypeOrSubtype() ->
                return serializableSerializer

            strict() ->
                throw IllegalArgumentException("Illegal type, could not find a specific serializer for ${irType.render()}")

            else ->
                return IrParcelSerializerWithClassLoader(parcelizeType, symbols.parcelReadValue, symbols.parcelWriteValue)
        }
    }

    private fun wrapNullableSerializerIfNeeded(irType: IrType, serializer: IrParcelSerializer) =
        if (irType.isNullable()) IrNullAwareParcelSerializer(serializer) else serializer

    private val irBuiltIns: IrBuiltIns
        get() = symbols.irBuiltIns

    private val stringArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateStringArray, symbols.parcelWriteStringArray)
    private val stringListSerializer = IrSimpleParcelSerializer(symbols.parcelCreateStringArrayList, symbols.parcelWriteStringList)
    private val stringPersistentListSerializer by lazy {
        IrExtensionFunctionOnReadCallingSerializer(
            delegated = stringListSerializer,
            converterExtensionFunction = symbols.kotlinIterableToPersistentListExtension,
        )
    }
    private val iBinderSerializer = IrSimpleParcelSerializer(symbols.parcelReadStrongBinder, symbols.parcelWriteStrongBinder)
    private val iBinderArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateBinderArray, symbols.parcelWriteBinderArray)
    private val iBinderListSerializer = IrSimpleParcelSerializer(symbols.parcelCreateBinderArrayList, symbols.parcelWriteBinderList)
    private val iBinderPersistentListSerializer by lazy {
        IrExtensionFunctionOnReadCallingSerializer(
            delegated = iBinderListSerializer,
            converterExtensionFunction = symbols.kotlinIterableToPersistentListExtension,
        )
    }
    private val serializableSerializer = IrSimpleParcelSerializer(symbols.parcelReadSerializable, symbols.parcelWriteSerializable)
    private val stringSerializer = IrSimpleParcelSerializer(symbols.parcelReadString, symbols.parcelWriteString)
    private val byteSerializer = IrSimpleParcelSerializer(symbols.parcelReadByte, symbols.parcelWriteByte)
    private val intSerializer = IrSimpleParcelSerializer(symbols.parcelReadInt, symbols.parcelWriteInt)
    private val longSerializer = IrSimpleParcelSerializer(symbols.parcelReadLong, symbols.parcelWriteLong)
    private val floatSerializer = IrSimpleParcelSerializer(symbols.parcelReadFloat, symbols.parcelWriteFloat)
    private val doubleSerializer = IrSimpleParcelSerializer(symbols.parcelReadDouble, symbols.parcelWriteDouble)
    private val intArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateIntArray, symbols.parcelWriteIntArray)
    private val booleanArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateBooleanArray, symbols.parcelWriteBooleanArray)
    private val byteArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateByteArray, symbols.parcelWriteByteArray)
    private val charArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateCharArray, symbols.parcelWriteCharArray)
    private val doubleArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateDoubleArray, symbols.parcelWriteDoubleArray)
    private val floatArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateFloatArray, symbols.parcelWriteFloatArray)
    private val longArraySerializer = IrSimpleParcelSerializer(symbols.parcelCreateLongArray, symbols.parcelWriteLongArray)

    // Primitive types without dedicated read/write methods need an additional cast.
    private val booleanSerializer = IrWrappedIntParcelSerializer(irBuiltIns.booleanType)
    private val shortSerializer = IrWrappedIntParcelSerializer(irBuiltIns.shortType)
    private val charSerializer = IrWrappedIntParcelSerializer(irBuiltIns.charType)
    private val shortArraySerializer = IrArrayParcelSerializer(
        irBuiltIns.primitiveArrayForType.getValue(irBuiltIns.shortType).defaultType,
        irBuiltIns.shortType,
        shortSerializer
    )

    // Unsigned primitive types
    private val ubyteSerializer = IrUnsafeCoerceWrappedSerializer(byteSerializer, symbols.kotlinUByte.defaultType, irBuiltIns.byteType)
    private val ushortSerializer = IrUnsafeCoerceWrappedSerializer(shortSerializer, symbols.kotlinUShort.defaultType, irBuiltIns.shortType)
    private val uintSerializer = IrUnsafeCoerceWrappedSerializer(intSerializer, symbols.kotlinUInt.defaultType, irBuiltIns.intType)
    private val ulongSerializer = IrUnsafeCoerceWrappedSerializer(longSerializer, symbols.kotlinULong.defaultType, irBuiltIns.longType)

    // Unsigned array types
    private val ubyteArraySerializer = IrUnsafeCoerceWrappedSerializer(
        byteArraySerializer,
        symbols.kotlinUByteArray.owner.defaultType,
        symbols.kotlinUByteArray.owner.inlineClassRepresentation!!.underlyingType
    )

    private val ushortArraySerializer = IrUnsafeCoerceWrappedSerializer(
        shortArraySerializer,
        symbols.kotlinUShortArray.owner.defaultType,
        symbols.kotlinUShortArray.owner.inlineClassRepresentation!!.underlyingType
    )

    private val uintArraySerializer = IrUnsafeCoerceWrappedSerializer(
        intArraySerializer,
        symbols.kotlinUIntArray.owner.defaultType,
        symbols.kotlinUIntArray.owner.inlineClassRepresentation!!.underlyingType
    )

    private val ulongArraySerializer = IrUnsafeCoerceWrappedSerializer(
        longArraySerializer,
        symbols.kotlinULongArray.owner.defaultType,
        symbols.kotlinULongArray.owner.inlineClassRepresentation!!.underlyingType
    )

    private val charSequenceSerializer = IrCharSequenceParcelSerializer()

    // TODO The old backend uses the hidden "read/writeRawFileDescriptor" methods.
    private val fileDescriptorSerializer = IrSimpleParcelSerializer(symbols.parcelReadFileDescriptor, symbols.parcelWriteFileDescriptor)

    private val sizeSerializer = IrSimpleParcelSerializer(symbols.parcelReadSize, symbols.parcelWriteSize)
    private val sizeFSerializer = IrSimpleParcelSerializer(symbols.parcelReadSizeF, symbols.parcelWriteSizeF)
    private val sparseBooleanArraySerializer =
        IrSimpleParcelSerializer(symbols.parcelReadSparseBooleanArray, symbols.parcelWriteSparseBooleanArray)

    // library types serializers
    private val durationSerializer = IrUnsafeCoerceWrappedSerializer(
        longSerializer,
        symbols.kotlinTimeDuration.defaultType,
        irBuiltIns.longType
    )
}
