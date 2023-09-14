/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.parcelize.serializers

import org.jetbrains.kotlin.codegen.FrameMap
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.parcelize.ParcelizeNames.RAW_VALUE_ANNOTATION_FQ_NAMES
import org.jetbrains.kotlin.parcelize.findAnyAnnotation
import org.jetbrains.kotlin.parcelize.hasAnyAnnotation
import org.jetbrains.kotlin.parcelize.isParcelize
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.synthetic.isVisibleOutside
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.isError
import org.jetbrains.kotlin.types.typeUtil.builtIns
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface ParcelSerializer {
    val asmType: Type

    fun writeValue(v: InstructionAdapter)
    fun readValue(v: InstructionAdapter)

    data class ParcelSerializerContext(
        val typeMapper: KotlinTypeMapper,
        val containerClassType: Type,
        val typeParcelers: List<TypeParcelerMapping>,
        val frameMap: FrameMap
    ) {
        fun findParcelerClass(type: KotlinType): KotlinType? {
            return typeParcelers.firstOrNull { it.mappedType == type }?.parcelerType
        }
    }

    companion object {
        private val WRITE_WITH_FQ_NAMES = listOf(
            FqName("kotlinx.parcelize.WriteWith"),
            FqName("kotlinx.android.parcel.WriteWith"),
        )

        private fun KotlinTypeMapper.mapTypeSafe(type: KotlinType, forceBoxed: Boolean) = when {
            type.isError -> Type.getObjectType("java/lang/Object")
            else -> mapType(type, null, if (forceBoxed) TypeMappingMode.GENERIC_ARGUMENT else TypeMappingMode.DEFAULT)
        }

        fun get(
            type: KotlinType,
            asmType: Type,
            context: ParcelSerializerContext,
            forceBoxed: Boolean = false,
            strict: Boolean = false
        ): ParcelSerializer {
            val typeMapper = context.typeMapper

            val className = asmType.className
            fun strict() = strict && !type.hasAnyAnnotation(RAW_VALUE_ANNOTATION_FQ_NAMES)

            fun findCustomParcelerType(type: KotlinType): KotlinType? {
                type.findAnyAnnotation(WRITE_WITH_FQ_NAMES)?.let { writeWith ->
                    val parceler = writeWith.type.arguments.singleOrNull()?.type
                    if (parceler != null && !parceler.isError) {
                        return parceler
                    }
                }

                return context.findParcelerClass(type)?.takeIf { !it.isError }
            }

            findCustomParcelerType(type)?.let { return TypeParcelerParcelSerializer(asmType, it, context.typeMapper) }

            return when {
                // Somewhat inconsistently, there are no `Parcel.writeShortArray/createShortArray` methods,
                // so we need to exclude "[S" here.
                asmType.descriptor == "[I"
                        || asmType.descriptor == "[Z"
                        || asmType.descriptor == "[B"
                        || asmType.descriptor == "[C"
                        || asmType.descriptor == "[D"
                        || asmType.descriptor == "[F"
                        || asmType.descriptor == "[J"
                -> {
                    val customElementParcelerType = findCustomParcelerType(type.builtIns.getArrayElementType(type))
                    if (customElementParcelerType != null) {
                        val elementType = asmType.elementType
                        val elementParceler = TypeParcelerParcelSerializer(elementType, customElementParcelerType, context.typeMapper)
                        ArrayParcelSerializer(asmType, elementParceler)
                    } else {
                        PrimitiveArrayParcelSerializer(asmType)
                    }
                }

                asmType.descriptor == "[Landroid/os/IBinder;" -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeBinderArray"), Method("createBinderArray")
                    )
                }

                asmType.descriptor == "[Ljava/lang/String;" -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeStringArray"), Method("createStringArray")
                    )
                }

                asmType.sort == Type.ARRAY -> {
                    val elementType = type.builtIns.getArrayElementType(type)
                    val elementSerializer =
                        get(elementType, typeMapper.mapTypeSafe(elementType, forceBoxed = true), context, strict = strict())

                    wrapToNullAwareIfNeeded(type, ArrayParcelSerializer(asmType, elementSerializer))
                }

                asmType.isPrimitive() -> {
                    if (forceBoxed || type.isMarkedNullable)
                        wrapToNullAwareIfNeeded(type, BoxedPrimitiveTypeParcelSerializer.forUnboxedType(asmType))
                    else
                        PrimitiveTypeParcelSerializer.getInstance(asmType)
                }

                asmType.isUnsigned() -> {
                    ParcelSerializerStub(asmType, type)
                }

                asmType.isString() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeString"),
                        Method("readString")
                    )
                }

                className == List::class.java.canonicalName
                        || className == ArrayList::class.java.canonicalName
                        || className == LinkedList::class.java.canonicalName
                        || className == Set::class.java.canonicalName
                        || className == SortedSet::class.java.canonicalName
                        || className == NavigableSet::class.java.canonicalName
                        || className == HashSet::class.java.canonicalName
                        || className == LinkedHashSet::class.java.canonicalName
                        || className == TreeSet::class.java.canonicalName
                -> {
                    val elementType = type.arguments.single().type
                    val elementAsmType = typeMapper.mapTypeSafe(elementType, forceBoxed = true)

                    if (className == List::class.java.canonicalName) {
                        // Don't care if the element type is nullable cause both writeStrongBinder() and writeString() support null values
                        if (elementAsmType.descriptor == "Landroid/os/IBinder;") {
                            return NullCompliantObjectParcelSerializer(
                                asmType,
                                Method("writeBinderList"), Method("createBinderArrayList", "()Ljava/util/ArrayList;")
                            )
                        } else if (elementAsmType.descriptor == "Ljava/lang/String;") {
                            return NullCompliantObjectParcelSerializer(
                                asmType,
                                Method("writeStringList"), Method("createStringArrayList", "()Ljava/util/ArrayList;")
                            )
                        }
                    }

                    val elementSerializer = get(elementType, elementAsmType, context, forceBoxed = true, strict = strict())
                    wrapToNullAwareIfNeeded(type, ListSetParcelSerializer(asmType, elementSerializer, context.frameMap))
                }

                className == Map::class.java.canonicalName
                        || className == SortedMap::class.java.canonicalName
                        || className == NavigableMap::class.java.canonicalName
                        || className == HashMap::class.java.canonicalName
                        || className == LinkedHashMap::class.java.canonicalName
                        || className == TreeMap::class.java.canonicalName
                        || className == ConcurrentHashMap::class.java.canonicalName
                -> {
                    val (keyType, valueType) = type.arguments.apply { assert(this.size == 2) }
                    val keySerializer = get(
                        keyType.type, typeMapper.mapTypeSafe(keyType.type, forceBoxed = true), context, forceBoxed = true, strict = strict()
                    )
                    val valueSerializer = get(
                        valueType.type,
                        typeMapper.mapTypeSafe(valueType.type, forceBoxed = true),
                        context,
                        forceBoxed = true,
                        strict = strict()
                    )
                    wrapToNullAwareIfNeeded(type, MapParcelSerializer(asmType, keySerializer, valueSerializer, context.frameMap))
                }

                asmType.isBoxedPrimitive() -> {
                    wrapToNullAwareIfNeeded(type, BoxedPrimitiveTypeParcelSerializer.forBoxedType(asmType))
                }

                asmType.isBlob() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeBlob"),
                        Method("readBlob")
                    )
                }

                asmType.isSize() -> {
                    wrapToNullAwareIfNeeded(
                        type, NullCompliantObjectParcelSerializer(
                            asmType,
                            Method("writeSize"),
                            Method("readSize")
                        )
                    )
                }

                asmType.isSizeF() -> wrapToNullAwareIfNeeded(
                    type, NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeSizeF"),
                        Method("readSizeF")
                    )
                )

                asmType.isBundle() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeBundle"),
                        Method("readBundle")
                    )
                }

                type.isIBinder() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeStrongBinder", "(Landroid/os/IBinder;)V"),
                        Method("readStrongBinder", "()Landroid/os/IBinder;")
                    )
                }

                type.isIInterface() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeStrongInterface", "(Landroid/os/IInterface;)V"),
                        Method("readStrongInterface", "()Landroid/os/IInterface;")
                    )
                }

                asmType.isPersistableBundle() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeBundle"),
                        Method("readBundle")
                    )
                }

                asmType.isSparseBooleanArray() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeSparseBooleanArray"),
                        Method("readSparseBooleanArray")
                    )
                }

                asmType.isSparseIntArray() -> {
                    wrapToNullAwareIfNeeded(
                        type, SparseArrayParcelSerializer(
                            asmType, PrimitiveTypeParcelSerializer.getInstance(Type.INT_TYPE), context.frameMap
                        )
                    )
                }

                asmType.isSparseLongArray() -> {
                    wrapToNullAwareIfNeeded(
                        type, SparseArrayParcelSerializer(
                            asmType, PrimitiveTypeParcelSerializer.getInstance(Type.LONG_TYPE), context.frameMap
                        )
                    )
                }

                asmType.isSparseArray() -> {
                    val elementType = type.arguments.single().type
                    val elementSerializer = get(
                        elementType, typeMapper.mapTypeSafe(elementType, forceBoxed = true), context, forceBoxed = true, strict = strict()
                    )
                    wrapToNullAwareIfNeeded(type, SparseArrayParcelSerializer(asmType, elementSerializer, context.frameMap))
                }

                type.isCharSequence() -> {
                    CharSequenceParcelSerializer(asmType)
                }

                type.isException() -> {
                    wrapToNullAwareIfNeeded(
                        type, NullCompliantObjectParcelSerializer(
                            asmType,
                            Method("writeException"),
                            Method("readException")
                        )
                    )
                }

                asmType.isFileDescriptor() -> {
                    wrapToNullAwareIfNeeded(
                        type, NullCompliantObjectParcelSerializer(
                            asmType,
                            Method("writeRawFileDescriptor"),
                            Method("readRawFileDescriptor")
                        )
                    )
                }

                // Write at least a nullability byte.
                // We don't want parcel to be empty in case if all constructor parameters are objects
                type.isNamedObject() -> {
                    NullAwareParcelSerializerWrapper(ObjectParcelSerializer(asmType, type, typeMapper))
                }

                type.isEnum() -> {
                    wrapToNullAwareIfNeeded(type, EnumParcelSerializer(asmType))
                }

                type.isParcelable() -> {
                    val clazz = type.constructor.declarationDescriptor as? ClassDescriptor
                    if (clazz != null && clazz.modality == Modality.FINAL && clazz.source is PsiSourceElement) {

                        fun MemberScope.findCreatorField() = getContributedVariables(
                            Name.identifier("CREATOR"), NoLookupLocation.WHEN_GET_ALL_DESCRIPTORS
                        ).firstOrNull()

                        val creatorVar = when (clazz) {
                            is JavaClassDescriptor -> clazz.staticScope.findCreatorField()
                            else -> clazz.companionObjectDescriptor?.unsubstitutedMemberScope?.findCreatorField()
                                ?.takeIf { it.annotations.hasAnnotation(FqName(JvmField::class.java.name)) }
                        }

                        val creatorAsmType = when {
                            creatorVar != null -> typeMapper.mapTypeSafe(creatorVar.type, forceBoxed = true)
                            clazz.isParcelize -> Type.getObjectType(asmType.internalName + "\$Creator")
                            else -> null
                        }

                        creatorAsmType?.let { wrapToNullAwareIfNeeded(type, EfficientParcelableParcelSerializer(asmType)) }
                            ?: GenericParcelableParcelSerializer(asmType, context.containerClassType)
                    } else {
                        GenericParcelableParcelSerializer(asmType, context.containerClassType)
                    }
                }

                type.isSerializable() -> {
                    NullCompliantObjectParcelSerializer(
                        asmType,
                        Method("writeSerializable", "(Ljava/io/Serializable;)V"),
                        Method("readSerializable", "()Ljava/io/Serializable;")
                    )
                }

                else -> {
                    if (strict && !type.hasAnyAnnotation(RAW_VALUE_ANNOTATION_FQ_NAMES))
                        throw IllegalArgumentException("Illegal type")
                    else
                        GenericParcelSerializer(asmType)
                }
            }
        }

        private fun wrapToNullAwareIfNeeded(type: KotlinType, serializer: ParcelSerializer): ParcelSerializer {
            return when {
                type.isMarkedNullable -> NullAwareParcelSerializerWrapper(serializer)
                else -> serializer
            }
        }

        private fun Type.isBlob() = this.sort == Type.ARRAY && this.elementType == Type.BYTE_TYPE
        private fun Type.isString() = this.descriptor == "Ljava/lang/String;"
        private fun Type.isSize() = this.descriptor == "Landroid/util/Size;"
        private fun Type.isSizeF() = this.descriptor == "Landroid/util/SizeF;"
        private fun Type.isFileDescriptor() = this.descriptor == "Ljava/io/FileDescriptor;"
        private fun Type.isBundle() = this.descriptor == "Landroid/os/Bundle;"
        private fun Type.isPersistableBundle() = this.descriptor == "Landroid/os/PersistableBundle;"
        private fun Type.isSparseBooleanArray() = this.descriptor == "Landroid/util/SparseBooleanArray;"
        private fun Type.isSparseIntArray() = this.descriptor == "Landroid/util/SparseIntArray;"
        private fun Type.isSparseLongArray() = this.descriptor == "Landroid/util/SparseLongArray;"
        private fun Type.isSparseArray() = this.descriptor == "Landroid/util/SparseArray;"
        private fun KotlinType.isException() = matchesFqNameWithSupertypes("java.lang.Exception")
        private fun KotlinType.isIBinder() = matchesFqNameWithSupertypes("android.os.IBinder")
        private fun KotlinType.isIInterface() = matchesFqNameWithSupertypes("android.os.IInterface")
        private fun KotlinType.isCharSequence() = matchesFqName("kotlin.CharSequence") || matchesFqName("java.lang.CharSequence")

        private fun KotlinType.isSerializable() = matchesFqNameWithSupertypes("java.io.Serializable")
                || matchesFqNameWithSupertypes("kotlin.Function")

        private fun KotlinType.isNamedObject(): Boolean {
            val classDescriptor = constructor.declarationDescriptor as? ClassDescriptor ?: return false
            if (!classDescriptor.visibility.isVisibleOutside()) return false
            if (DescriptorUtils.isAnonymousObject(classDescriptor)) return false
            return classDescriptor.kind == ClassKind.OBJECT
        }

        private fun KotlinType.isEnum() = (constructor.declarationDescriptor as? ClassDescriptor)?.kind == ClassKind.ENUM_CLASS

        private fun Type.isPrimitive(): Boolean = when (this.sort) {
            Type.BOOLEAN, Type.CHAR, Type.BYTE, Type.SHORT, Type.INT, Type.FLOAT, Type.LONG, Type.DOUBLE -> true
            else -> false
        }

        private fun Type.isUnsigned(): Boolean = when (descriptor) {
            "Lkotlin/UByte;", "Lkotlin/UShort;", "Lkotlin/UInt;", "Lkotlin/ULong;",
            "Lkotlin/UByteArray;", "Lkotlin/UShortArray;", "Lkotlin/UIntArray;", "Lkotlin/ULongArray;" -> true
            else -> false
        }

        private fun Type.isBoxedPrimitive(): Boolean = when (this.descriptor) {
            "Ljava/lang/Boolean;",
            "Ljava/lang/Character;",
            "Ljava/lang/Byte;",
            "Ljava/lang/Short;",
            "Ljava/lang/Integer;",
            "Ljava/lang/Float;",
            "Ljava/lang/Long;",
            "Ljava/lang/Double;"
            -> true
            else -> false
        }
    }
}
