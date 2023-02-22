/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages
import org.jetbrains.kotlinx.serialization.compiler.resolve.SpecialBuiltins
import org.jetbrains.kotlinx.serialization.compiler.resolve.*
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.objectSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializersClassIds.referenceArraySerializerId

class IrSerialTypeInfo(
    val property: IrSerializableProperty,
    val elementMethodPrefix: String,
    val serializer: IrClassSymbol? = null
)

interface SerializationBaseContext {
    fun referenceClassId(classId: ClassId): IrClassSymbol?

    val runtimeHasEnumSerializerFactoryFunctions: Boolean
}

fun BaseIrGenerator.getIrSerialTypeInfo(property: IrSerializableProperty, ctx: SerializationBaseContext): IrSerialTypeInfo {
    fun SerializableInfo(serializer: IrClassSymbol?) =
        IrSerialTypeInfo(property, if (property.type.isNullable()) "Nullable" else "", serializer)

    val T = property.type
    property.serializableWith(ctx)?.let { return SerializableInfo(it) }
    findAddOnSerializer(T, ctx)?.let { return SerializableInfo(it) }
    T.overriddenSerializer?.let { return SerializableInfo(it) }
    return when {
        T.isTypeParameter() -> IrSerialTypeInfo(property, if (property.type.isMarkedNullable()) "Nullable" else "", null)
        T.isPrimitiveType() -> IrSerialTypeInfo(
            property,
            T.classFqName!!.asString().removePrefix("kotlin.")
        )
        T.isString() -> IrSerialTypeInfo(property, "String")
        T.isArray() -> {
            val serializer = property.serializableWith(ctx) ?: ctx.getClassFromInternalSerializationPackage(SpecialBuiltins.referenceArraySerializer)
            SerializableInfo(serializer)
        }
        else -> {
            val serializer =
                findTypeSerializerOrContext(ctx, property.type)
            SerializableInfo(serializer)
        }
    }
}

fun BaseIrGenerator.findAddOnSerializer(propertyType: IrType, ctx: SerializationBaseContext): IrClassSymbol? {
    val classSymbol = propertyType.classOrNull ?: return null
    additionalSerializersInScopeOfCurrentFile[classSymbol to propertyType.isNullable()]?.let { return it }
    if (classSymbol in contextualKClassListInCurrentFile)
        return ctx.getClassFromRuntime(SpecialBuiltins.contextSerializer)
    if (classSymbol.owner.annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName))
        return ctx.getClassFromRuntime(SpecialBuiltins.polymorphicSerializer)
    if (propertyType.isNullable()) return findAddOnSerializer(propertyType.makeNotNull(), ctx)
    return null
}

fun BaseIrGenerator?.findTypeSerializerOrContext(
    context: SerializationBaseContext, kType: IrType
): IrClassSymbol? {
    if (kType.isTypeParameter()) return null
    return findTypeSerializerOrContextUnchecked(context, kType) ?: error("Serializer for element of type ${kType.render()} has not been found")
}

fun BaseIrGenerator?.findTypeSerializerOrContextUnchecked(
    context: SerializationBaseContext, kType: IrType
): IrClassSymbol? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith()?.let { return it }
    this?.additionalSerializersInScopeOfCurrentFile?.get(kType.classOrNull!! to kType.isNullable())?.let {
        return it
    }
    if (kType.isMarkedNullable()) return findTypeSerializerOrContextUnchecked(context, kType.makeNotNull())
    if (this?.contextualKClassListInCurrentFile?.contains(kType.classOrNull) == true) return context.referenceClassId(contextSerializerId)
    return analyzeSpecialSerializers(context, annotations) ?: findTypeSerializer(context, kType)
}

fun analyzeSpecialSerializers(
    context: SerializationBaseContext,
    annotations: List<IrConstructorCall>
): IrClassSymbol? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualFqName) || annotations.hasAnnotation(SerializationAnnotations.contextualOnPropertyFqName) ->
        context.referenceClassId(contextSerializerId)
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName) ->
        context.referenceClassId(polymorphicSerializerId)
    else -> null
}


fun findTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    type.overriddenSerializer?.let { return it }
    if (type.isTypeParameter()) return null
    if (type.isArray()) return context.referenceClassId(referenceArraySerializerId)
    if (type.isGeneratedSerializableObject()) return context.referenceClassId(objectSerializerId)
    val stdSer = findStandardKotlinTypeSerializer(context, type) // see if there is a standard serializer
        ?: findEnumTypeSerializer(context, type)
    if (stdSer != null) return stdSer
    if (type.isInterface() && type.classOrNull?.owner?.isSealedSerializableInterface == false) return context.referenceClassId(
        polymorphicSerializerId
    )
    return type.classOrNull?.owner.classSerializer(context) // check for serializer defined on the type
}

fun findEnumTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    val classSymbol = type.classOrNull?.owner ?: return null

    // in any case, the function returns the serializer for the enum
    if (classSymbol.kind != ClassKind.ENUM_CLASS) return null

    val legacySerializer = classSymbol.findEnumLegacySerializer()
    // $serializer for legacy compiled enums, or EnumSerializer for factories
    return legacySerializer?.symbol ?: context.referenceClassId(enumSerializerId)
}

internal fun IrClass?.classSerializer(context: SerializationBaseContext): IrClassSymbol? = this?.let {
    // serializer annotation on class?
    serializableWith?.let { return it }
    // companion object serializer?
    if (hasCompanionObjectAsSerializer) return companionObject()?.symbol
    // can infer @Poly?
    polymorphicSerializerIfApplicableAutomatically(context)?.let { return it }
    // default serializable?
    if (shouldHaveGeneratedSerializer()) {
        // $serializer nested class
        return this.declarations
            .filterIsInstance<IrClass>()
            .singleOrNull { it.name == SerialEntityNames.SERIALIZER_CLASS_NAME }?.symbol
    }
    return null
}

internal fun IrClass.polymorphicSerializerIfApplicableAutomatically(context: SerializationBaseContext): IrClassSymbol? {
    val serializer = when {
        kind == ClassKind.INTERFACE && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        kind == ClassKind.INTERFACE -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        else -> null
    }
    return serializer?.let {
        context.getClassFromRuntimeOrNull(
            it,
            SerializationPackages.packageFqName,
            SerializationPackages.internalPackageFqName
        )
    }
}

internal val IrType.overriddenSerializer: IrClassSymbol?
    get() {
        annotations.serializableWith()?.let { return it }
        val desc = this.classOrNull ?: return null
        desc.owner.serializableWith?.let { return it }
        return null
    }

internal val IrClass.serializableWith: IrClassSymbol?
    get() = annotations.serializableWith()

internal val IrClass.serializerForClass: IrClassSymbol?
    get() = (annotations.findAnnotation(SerializationAnnotations.serializerAnnotationFqName)
        ?.getValueArgument(0) as? IrClassReference)?.symbol as? IrClassSymbol

fun findStandardKotlinTypeSerializer(context: SerializationBaseContext, type: IrType): IrClassSymbol? {
    val typeName = type.classFqName?.toString()
    val name = when (typeName) {
        "Z" -> if (type.isBoolean()) "BooleanSerializer" else null
        "B" -> if (type.isByte()) "ByteSerializer" else null
        "S" -> if (type.isShort()) "ShortSerializer" else null
        "I" -> if (type.isInt()) "IntSerializer" else null
        "J" -> if (type.isLong()) "LongSerializer" else null
        "F" -> if (type.isFloat()) "FloatSerializer" else null
        "D" -> if (type.isDouble()) "DoubleSerializer" else null
        "C" -> if (type.isChar()) "CharSerializer" else null
        null -> null
        else -> findStandardKotlinTypeSerializerName(typeName)
    } ?: return null
    return context.getClassFromRuntimeOrNull(name, SerializationPackages.internalPackageFqName, SerializationPackages.packageFqName)
}

// @Serializable(X::class) -> X
internal fun List<IrConstructorCall>.serializableWith(): IrClassSymbol? {
    val annotation = findAnnotation(SerializationAnnotations.serializableAnnotationFqName) ?: return null
    val arg = annotation.getValueArgument(0) as? IrClassReference ?: return null
    return arg.symbol as? IrClassSymbol
}

internal fun getSerializableClassByCompanion(companionClass: IrClass): IrClass? {
    if (companionClass.isSerializableObject) return companionClass
    if (!companionClass.isCompanion) return null
    val classDescriptor = (companionClass.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}

fun BaseIrGenerator?.allSealedSerializableSubclassesFor(
    irClass: IrClass,
    context: SerializationBaseContext
): Pair<List<IrSimpleType>, List<IrClassSymbol>> {
    assert(irClass.modality == Modality.SEALED)
    fun recursiveSealed(klass: IrClass): Collection<IrClass> {
        return klass.sealedSubclasses.map { it.owner }.flatMap { if (it.modality == Modality.SEALED) recursiveSealed(it) else setOf(it) }
    }

    val serializableSubtypes = recursiveSealed(irClass).map { it.defaultType }
    return serializableSubtypes.mapNotNull { subtype ->
        findTypeSerializerOrContextUnchecked(context, subtype)?.let { Pair(subtype, it) }
    }.unzip()
}

internal fun SerializationBaseContext.getSerializableClassDescriptorBySerializer(serializer: IrClass): IrClass? {
    val serializerForClass = serializer.serializerForClass
    if (serializerForClass != null) return serializerForClass.owner
    if (serializer.name !in setOf(
            SerialEntityNames.SERIALIZER_CLASS_NAME,
            SerialEntityNames.GENERATED_SERIALIZER_CLASS
        )
    ) return null
    val classDescriptor = (serializer.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedSerializer()) return null
    return classDescriptor
}

fun SerializationBaseContext.getClassFromRuntimeOrNull(className: String, vararg packages: FqName): IrClassSymbol? {
    val listToSearch = if (packages.isEmpty()) SerializationPackages.allPublicPackages else packages.toList()
    for (pkg in listToSearch) {
        referenceClassId(ClassId(pkg, Name.identifier(className)))?.let { return it }
    }
    return null
}

fun SerializationBaseContext.getClassFromRuntime(className: String, vararg packages: FqName): IrClassSymbol {
    return getClassFromRuntimeOrNull(className, *packages) ?: error(
        "Class $className wasn't found in ${packages.toList().ifEmpty { SerializationPackages.allPublicPackages }}. " +
                "Check that you have correct version of serialization runtime in classpath."
    )
}

fun SerializationBaseContext.getClassFromInternalSerializationPackage(className: String): IrClassSymbol =
    getClassFromRuntimeOrNull(className, SerializationPackages.internalPackageFqName)
        ?: error("Class $className wasn't found in ${SerializationPackages.internalPackageFqName}. Check that you have correct version of serialization runtime in classpath.")

