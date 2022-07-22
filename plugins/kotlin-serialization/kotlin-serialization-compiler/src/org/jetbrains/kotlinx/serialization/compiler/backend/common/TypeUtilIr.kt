/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.common

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.CompilationException
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.contextSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.enumSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.objectSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.polymorphicSerializerId
import org.jetbrains.kotlinx.serialization.compiler.backend.jvm.referenceArraySerializerId
import org.jetbrains.kotlinx.serialization.compiler.extensions.SerializationPluginContext
import org.jetbrains.kotlinx.serialization.compiler.fir.SerializationPluginKey
import org.jetbrains.kotlinx.serialization.compiler.resolve.*

fun AbstractSerialGenerator.findTypeSerializerOrContextUnchecked(
    context: SerializationPluginContext, kType: IrType
): IrClassSymbol? {
    val annotations = kType.annotations
    if (kType.isTypeParameter()) return null
    annotations.serializableWith()?.let { return it.classOrNull }
    additionalSerializersInScopeOfCurrentFile[kType.classOrNull?.descriptor to kType.isMarkedNullable()]?.let {
        return context.referenceClass(
            ClassId.topLevel(it.fqNameSafe)
        )
    }
    if (kType.isMarkedNullable()) return findTypeSerializerOrContextUnchecked(context, kType.makeNotNull())
    if (kType.toKotlinType() in contextualKClassListInCurrentFile) return context.referenceClass(contextSerializerId)
    return analyzeSpecialSerializers(context, annotations) ?: findTypeSerializer(context, kType)
}

fun analyzeSpecialSerializers(
    context: SerializationPluginContext,
    annotations: List<IrConstructorCall>
): IrClassSymbol? = when {
    annotations.hasAnnotation(SerializationAnnotations.contextualFqName) || annotations.hasAnnotation(SerializationAnnotations.contextualOnPropertyFqName) ->
        context.referenceClass(contextSerializerId)
    // can be annotation on type usage, e.g. List<@Polymorphic Any>
    annotations.hasAnnotation(SerializationAnnotations.polymorphicFqName) ->
        context.referenceClass(polymorphicSerializerId)
    else -> null
}

fun AbstractSerialGenerator.findTypeSerializerOrContext(
    context: SerializationPluginContext, kType: IrType,
    sourceElement: PsiElement? = null
): IrClassSymbol? {
    if (kType.isTypeParameter()) return null
    return findTypeSerializerOrContextUnchecked(context, kType) ?: throw CompilationException(
        "Serializer for element of type $kType has not been found.\n" +
                "To use context serializer as fallback, explicitly annotate element with @Contextual",
        null,
        sourceElement
    )
}

fun findTypeSerializer(context: SerializationPluginContext, type: IrType): IrClassSymbol? {
    val userOverride = type.overridenSerializer
    if (userOverride != null) return userOverride.classOrNull
    if (type.isTypeParameter()) return null
    if (type.isArray()) return context.referenceClass(referenceArraySerializerId)
    if (type.isGeneratedSerializableObject()) return context.referenceClass(objectSerializerId)
    val stdSer = findStandardKotlinTypeSerializer(context, type) // see if there is a standard serializer
        ?: findEnumTypeSerializer(context, type)
    if (stdSer != null) return stdSer
    if (type.isInterface() && type.classOrNull?.owner?.isSealedSerializableInterface == false) return context.referenceClass(
        polymorphicSerializerId
    )
    return type.classOrNull?.owner.classSerializer(context) // check for serializer defined on the type
}

internal fun IrClass?.classSerializer(context: SerializationPluginContext): IrClassSymbol? = this?.let {
    // serializer annotation on class?
    serializableWith?.let { return it.classOrNull }
    // companion object serializer?
    if (hasCompanionObjectAsSerializer) return companionObject()?.symbol
    // can infer @Poly?
    polymorphicSerializerIfApplicableAutomatically(context)?.let { return it }
    // default serializable?
    if (shouldHaveGeneratedSerializer) {
        // $serializer nested class
        return this.declarations
            .filterIsInstance<IrClass>()
            .singleOrNull { it.name == SerialEntityNames.SERIALIZER_CLASS_NAME }?.symbol
    }
    return null
}

internal val IrClass.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN))
            || isEnumWithLegacyGeneratedSerializer()

internal fun IrClass.polymorphicSerializerIfApplicableAutomatically(context: SerializationPluginContext): IrClassSymbol? {
    val serializer = when {
        kind == ClassKind.INTERFACE && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        kind == ClassKind.INTERFACE -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.ABSTRACT -> SpecialBuiltins.polymorphicSerializer
        isInternalSerializable && modality == Modality.SEALED -> SpecialBuiltins.sealedSerializer
        else -> null
    }
    return serializer?.let { context.getClassFromRuntimeOrNull(it, SerializationPackages.packageFqName, SerializationPackages.internalPackageFqName) }
}

internal val IrClass.isInternalSerializable: Boolean
    get() {
        if (kind != ClassKind.CLASS) return false
        return hasSerializableOrMetaAnnotationWithoutArgs()
    }

internal val IrClass.isAbstractOrSealedSerializableClass: Boolean get() = isInternalSerializable && (modality == Modality.ABSTRACT || modality == Modality.SEALED)

internal val IrClass.isStaticSerializable: Boolean get() = this.typeParameters.isEmpty()


internal val IrClass.hasCompanionObjectAsSerializer: Boolean
    get() = isInternallySerializableObject || companionObject()?.serializerForClass == this.defaultType

internal val IrClass.isInternallySerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs()

internal val IrClass.serializerForClass: IrType?
    get() = null // TODO("Serializer(forClass)")

fun findEnumTypeSerializer(context: SerializationPluginContext, type: IrType): IrClassSymbol? {
    val classSymbol = type.classOrNull?.owner ?: return null
    return if (classSymbol.kind == ClassKind.ENUM_CLASS && !classSymbol.isEnumWithLegacyGeneratedSerializer())
        context.referenceClass(enumSerializerId)
    else null
}

internal fun IrClass.isEnumWithLegacyGeneratedSerializer(): Boolean = isInternallySerializableEnum() && useGeneratedEnumSerializer

internal val IrClass.useGeneratedEnumSerializer: Boolean
    get() = true // todo ????

internal val IrClass.isSealedSerializableInterface: Boolean
    get() = kind == ClassKind.INTERFACE && modality == Modality.SEALED && hasSerializableOrMetaAnnotationWithoutArgs() // in previous version, it was just 'serializableOrMeta'

internal fun IrClass.isInternallySerializableEnum(): Boolean =
    kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotationWithoutArgs()

fun findStandardKotlinTypeSerializer(context: SerializationPluginContext, type: IrType): IrClassSymbol? {
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
        else -> findStandardKotlinTypeSerializer(typeName)
    } ?: return null
    return context.getClassFromRuntimeOrNull(name, SerializationPackages.internalPackageFqName, SerializationPackages.packageFqName)
}

fun IrType.isGeneratedSerializableObject(): Boolean {
    return classOrNull?.run { owner.kind == ClassKind.OBJECT && owner.hasSerializableOrMetaAnnotationWithoutArgs() } == true
}

internal val IrClass.isSerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotation()

// todo: optimize & unify
internal fun IrClass.hasSerializableOrMetaAnnotationWithoutArgs(): Boolean {
    val annot = getAnnotation(SerializationAnnotations.serializableAnnotationFqName)
    if (annot != null) {
        for (i in 0 until annot.valueArgumentsCount) {
            if (annot.getValueArgument(i) != null) return false
        }
        return true
    }
    val metaAnnotation = annotations
        .flatMap { it.symbol.owner.constructedClass.annotations }
        .find { it.isAnnotation(SerializationAnnotations.metaSerializableAnnotationFqName) }
    return metaAnnotation != null
}

fun SerializationPluginContext.getClassFromRuntimeOrNull(className: String, vararg packages: FqName): IrClassSymbol? {
    val listToSearch = if (packages.isEmpty()) SerializationPackages.allPublicPackages else packages.toList()
    for (pkg in listToSearch) {
        referenceClass(ClassId(pkg, Name.identifier(className)))?.let { return it }
    }
    return null
}

fun SerializationPluginContext.getClassFromRuntime(className: String, vararg packages: FqName): IrClassSymbol {
    return getClassFromRuntimeOrNull(className, *packages) ?:
    error("Class $className wasn't found in ${packages.toList().ifEmpty { SerializationPackages.allPublicPackages }}. " +
                  "Check that you have correct version of serialization runtime in classpath.")
}

fun SerializationPluginContext.getClassFromInternalSerializationPackage(className: String): IrClassSymbol =
    getClassFromRuntimeOrNull(className, SerializationPackages.internalPackageFqName)
        ?: error("Class $className wasn't found in ${SerializationPackages.internalPackageFqName}. Check that you have correct version of serialization runtime in classpath.")


internal val IrType.overridenSerializer: IrSimpleType?
    get() {
        val desc = this.classOrNull ?: return null
        desc.owner.serializableWith?.let { return it }
        return null
    }

internal val IrClass.serializableWith: IrSimpleType?
    get() = annotations.serializableWith()


internal fun List<IrConstructorCall>.serializableWith(): IrSimpleType? {
    // XXX::class
//    val annotationArg =
//        firstOrNull { it.type.classFqName == SerializationAnnotations.serializerAnnotationFqName }?.getValueArgument(0) ?: return null
    // TODO("SerializableWith")
    return null
}

internal fun getSerializableClassByCompanion(companionClass: IrClass): IrClass? {
    if (companionClass.isSerializableObject) return companionClass
    if (!companionClass.isCompanion) return null
    val classDescriptor = (companionClass.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedMethodsInCompanion) return null
    return classDescriptor
}

internal val IrClass.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum() || (this.kind == ClassKind.CLASS && hasSerializableOrMetaAnnotation()) || this.isSealedSerializableInterface

internal fun IrClass.isSerializableEnum(): Boolean = kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotation()

fun IrClass.hasSerializableOrMetaAnnotation() = descriptor.hasSerializableOrMetaAnnotation // TODO

internal val IrType.genericIndex: Int?
    get() = (this.classifierOrNull as? IrTypeParameterSymbol)?.owner?.index

fun IrType.serialName(): String  = this.classOrNull!!.owner.serialName()
fun IrClass.serialName(): String {
    return descriptor.serialName() // TODO
}

fun AbstractSerialGenerator.allSealedSerializableSubclassesFor(
    irClass: IrClass,
    context: SerializationPluginContext
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

fun IrClass.findEnumValuesMethod() = this.functions.singleOrNull { f ->
    f.name == Name.identifier("values") && f.valueParameters.isEmpty() && f.extensionReceiverParameter == null
} ?: throw AssertionError("Enum class does not have single .values() function")

internal fun IrClass.enumEntries(): List<IrEnumEntry> {
    check(this.kind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<IrEnumEntry>()
//        .filter { it.kind == ClassKind.ENUM_ENTRY }
        .toList()
}

internal fun IrClass.isEnumWithSerialInfoAnnotation(): Boolean {
    if (kind != ClassKind.ENUM_CLASS) return false
    if (annotations.hasAnySerialAnnotation) return true
    return enumEntries().any { (it.annotations.hasAnySerialAnnotation) }
}

internal val List<IrConstructorCall>.hasAnySerialAnnotation: Boolean
    get() = false // TODO serialNameValue != null || any { it.annotationClass?.isSerialInfoAnnotation == true }

internal val List<IrConstructorCall>.serialNameValue: String?
    get() = null // todo("List<IrConstructorCall>.serialNameValue") @SerialName

internal fun getSerializableClassDescriptorBySerializer(serializer: IrClass): IrClass? {
    val serializerForClass = serializer.serializerForClass
    if (serializerForClass != null) return serializerForClass.classOrNull?.owner
    if (serializer.name !in setOf(
            SerialEntityNames.SERIALIZER_CLASS_NAME,
            SerialEntityNames.GENERATED_SERIALIZER_CLASS
        )
    ) return null
    val classDescriptor = (serializer.parent as? IrClass) ?: return null
    if (!classDescriptor.shouldHaveGeneratedSerializer) return null
    return classDescriptor
}

internal fun isKSerializer(type: IrType): Boolean {
    val simpleType = type as? IrSimpleType ?: return false
    val classifier = simpleType.classifier as? IrClassSymbol ?: return false
    val fqName = classifier.owner.fqNameWhenAvailable
    return fqName == SerialEntityNames.KSERIALIZER_NAME_FQ || fqName == SerialEntityNames.GENERATED_SERIALIZER_FQ
}

internal fun IrClass.findPluginGeneratedMethod(name: String): IrSimpleFunction? {
    return this.functions.find { it.name.asString() == name && it.origin == IrDeclarationOrigin.GeneratedByPlugin(SerializationPluginKey) }
}

internal fun IrConstructor.isSerializationCtor(): Boolean {
    /*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == SerialEntityNames.dummyParamName && type.classFqName == SerializationPackages.internalPackageFqName.child(
            SerialEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    } == true
}

internal fun IrExpression.isInitializePropertyFromParameter(): Boolean = this is IrGetValueImpl && this.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER