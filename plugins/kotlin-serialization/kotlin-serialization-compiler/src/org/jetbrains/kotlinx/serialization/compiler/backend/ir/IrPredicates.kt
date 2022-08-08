/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.backend.ir

import org.jetbrains.kotlin.backend.jvm.ir.getStringConstArgument
import org.jetbrains.kotlin.backend.jvm.ir.representativeUpperBound
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetEnumValue
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrGetValueImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.IrScriptSymbol
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlinx.serialization.compiler.fir.SerializationPluginKey
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerialEntityNames
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationAnnotations
import org.jetbrains.kotlinx.serialization.compiler.resolve.SerializationPackages

internal fun IrType.isKSerializer(): Boolean {
    val simpleType = this as? IrSimpleType ?: return false
    val classifier = simpleType.classifier as? IrClassSymbol ?: return false
    val fqName = classifier.owner.fqNameWhenAvailable
    return fqName == SerialEntityNames.KSERIALIZER_NAME_FQ || fqName == SerialEntityNames.GENERATED_SERIALIZER_FQ
}

internal val IrClass.isInternalSerializable: Boolean
    get() {
        if (kind != ClassKind.CLASS) return false
        return hasSerializableOrMetaAnnotationWithoutArgs()
    }

internal val IrClass.isAbstractOrSealedSerializableClass: Boolean get() = isInternalSerializable && (modality == Modality.ABSTRACT || modality == Modality.SEALED)

internal val IrClass.isStaticSerializable: Boolean get() = this.typeParameters.isEmpty()


internal val IrClass.hasCompanionObjectAsSerializer: Boolean
    get() = isInternallySerializableObject || companionObject()?.serializerForClass == this.symbol

internal val IrClass.isInternallySerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotationWithoutArgs()


internal fun IrClass.findPluginGeneratedMethod(name: String): IrSimpleFunction? {
    return this.functions.find {
        it.name.asString() == name && it.isFromPlugin()
    }
}

internal fun IrClass.isEnumWithLegacyGeneratedSerializer(): Boolean = isInternallySerializableEnum() && useGeneratedEnumSerializer

internal val IrClass.useGeneratedEnumSerializer: Boolean
    get() = true // FIXME This would break if we try to use this new IR compiler with pre-1.4.1 serialization versions. I think we just need to raise min runtime version.

internal val IrClass.isSealedSerializableInterface: Boolean
    get() = kind == ClassKind.INTERFACE && modality == Modality.SEALED && hasSerializableOrMetaAnnotation()

internal fun IrClass.isInternallySerializableEnum(): Boolean =
    kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotationWithoutArgs()

fun IrType.isGeneratedSerializableObject(): Boolean {
    return classOrNull?.run { owner.kind == ClassKind.OBJECT && owner.hasSerializableOrMetaAnnotationWithoutArgs() } == true
}

internal val IrClass.isSerializableObject: Boolean
    get() = kind == ClassKind.OBJECT && hasSerializableOrMetaAnnotation()

internal fun IrClass.hasSerializableOrMetaAnnotationWithoutArgs(): Boolean = checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs = true)

fun IrClass.hasSerializableOrMetaAnnotation() = checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs = false)

private fun IrClass.checkSerializableOrMetaAnnotationArgs(mustDoNotHaveArgs: Boolean): Boolean {
    val annot = getAnnotation(SerializationAnnotations.serializableAnnotationFqName)
    if (annot != null) { // @Serializable have higher priority
        if (!mustDoNotHaveArgs) return true
        if (annot.getValueArgument(0) != null) return false
        return true
    }
    return annotations
        .map { it.annotationClass.annotations }
        .any { it.hasAnnotation(SerializationAnnotations.metaSerializableAnnotationFqName) }
}

internal val IrClass.isSerialInfoAnnotation: Boolean
    get() = annotations.hasAnnotation(SerializationAnnotations.serialInfoFqName)
            || annotations.hasAnnotation(SerializationAnnotations.inheritableSerialInfoFqName)
            || annotations.hasAnnotation(SerializationAnnotations.metaSerializableAnnotationFqName)

internal val IrClass.shouldHaveGeneratedSerializer: Boolean
    get() = (isInternalSerializable && (modality == Modality.FINAL || modality == Modality.OPEN))
            || isEnumWithLegacyGeneratedSerializer()

internal val IrClass.shouldHaveGeneratedMethodsInCompanion: Boolean
    get() = this.isSerializableObject || this.isSerializableEnum() || (this.kind == ClassKind.CLASS && hasSerializableOrMetaAnnotation()) || this.isSealedSerializableInterface

internal fun IrClass.isSerializableEnum(): Boolean = kind == ClassKind.ENUM_CLASS && hasSerializableOrMetaAnnotation()

internal val IrType.genericIndex: Int?
    get() = (this.classifierOrNull as? IrTypeParameterSymbol)?.owner?.index

fun IrType.serialName(): String = this.classOrUpperBound()!!.owner.serialName()

fun IrClass.serialName(): String {
    return annotations.serialNameValue ?: fqNameWhenAvailable?.asString() ?: error("${this.render()} does not have fqName")
}

fun IrClass.findEnumValuesMethod() = this.functions.singleOrNull { f ->
    f.name == Name.identifier("values") && f.valueParameters.isEmpty() && f.extensionReceiverParameter == null
} ?: throw AssertionError("Enum class does not have single .values() function")

internal fun IrClass.enumEntries(): List<IrEnumEntry> {
    check(this.kind == ClassKind.ENUM_CLASS)
    return declarations.filterIsInstance<IrEnumEntry>().toList()
}

internal fun IrClass.isEnumWithSerialInfoAnnotation(): Boolean {
    if (kind != ClassKind.ENUM_CLASS) return false
    if (annotations.hasAnySerialAnnotation) return true
    return enumEntries().any { (it.annotations.hasAnySerialAnnotation) }
}

fun IrClass.findWriteSelfMethod(): IrSimpleFunction? =
    functions.singleOrNull { it.name == SerialEntityNames.WRITE_SELF_NAME && !it.isFakeOverride }

fun IrClass.getSuperClassNotAny(): IrClass? {
    val parentClass =
        superTypes
            .mapNotNull { it.classOrNull?.owner }
            .singleOrNull { it.kind == ClassKind.CLASS || it.kind == ClassKind.ENUM_CLASS } ?: return null
    return if (parentClass.defaultType.isAny()) null else parentClass
}

internal fun IrDeclaration.isFromPlugin(): Boolean =
    this.origin == IrDeclarationOrigin.GeneratedByPlugin(SerializationPluginKey) || (this.descriptor as? CallableMemberDescriptor)?.kind == CallableMemberDescriptor.Kind.SYNTHESIZED // old FE doesn't specify origin

internal fun IrConstructor.isSerializationCtor(): Boolean {
    /*kind == CallableMemberDescriptor.Kind.SYNTHESIZED does not work because DeserializedClassConstructorDescriptor loses its kind*/
    return valueParameters.lastOrNull()?.run {
        name == SerialEntityNames.dummyParamName && type.classFqName == SerializationPackages.internalPackageFqName.child(
            SerialEntityNames.SERIAL_CTOR_MARKER_NAME
        )
    } == true
}


internal fun IrConstructor.lastArgumentIsAnnotationArray(): Boolean {
    val lastArgType = valueParameters.lastOrNull()?.type
    if (lastArgType == null || !lastArgType.isArray()) return false
    return ((lastArgType as? IrSimpleType)?.arguments?.firstOrNull()?.typeOrNull?.classFqName?.toString() == "kotlin.Annotation")
}

fun IrClass.serializableSyntheticConstructor(): IrConstructorSymbol? { // FIXME: remove nullability W/A when FIR plugin will add proper deserialization ctor
    return declarations.filterIsInstance<IrConstructor>().singleOrNull { it.isSerializationCtor() }?.symbol
}

internal fun IrExpression.isInitializePropertyFromParameter(): Boolean =
    this is IrGetValueImpl && this.origin == IrStatementOrigin.INITIALIZE_PROPERTY_FROM_PARAMETER

internal val IrConstructorCall.annotationClass
    get() = this.symbol.owner.constructedClass

internal val List<IrConstructorCall>.hasAnySerialAnnotation: Boolean
    get() = serialNameValue != null || any { it.annotationClass.isSerialInfoAnnotation == true }

internal val List<IrConstructorCall>.serialNameValue: String?
    get() = findAnnotation(SerializationAnnotations.serialNameAnnotationFqName)?.getStringConstArgument(0) // @SerialName("foo")

/**
 * True — ALWAYS
 * False — NEVER
 * null — not specified
 */
fun IrProperty.getEncodeDefaultAnnotationValue(): Boolean? {
    val call = annotations.findAnnotation(SerializationAnnotations.encodeDefaultFqName) ?: return null
    val arg = call.getValueArgument(0) ?: return true // ALWAYS by default
    val argValue = (arg as? IrGetEnumValue
        ?: error("Argument of enum constructor expected to implement IrGetEnumValue, got $arg")).symbol.owner.name.toString()
    return when (argValue) {
        "ALWAYS" -> true
        "NEVER" -> false
        else -> error("Unknown EncodeDefaultMode enum value: $argValue")
    }
}

fun findSerializerConstructorForTypeArgumentsSerializers(serializer: IrClass): IrConstructorSymbol? {
    val typeParamsCount = ((serializer.superTypes.find { it.isKSerializer() } as IrSimpleType).arguments.first().typeOrNull!! as IrSimpleType).arguments.size
    if (typeParamsCount == 0) return null //don't need it

    return serializer.constructors.singleOrNull {
        it.valueParameters.let { vps -> vps.size == typeParamsCount && vps.all { vp -> vp.type.isKSerializer() } }
    }?.symbol
}

fun IrType.classOrUpperBound(): IrClassSymbol? = when(val cls = classifierOrNull) {
    is IrClassSymbol -> cls
    is IrScriptSymbol -> cls.owner.targetClass
    is IrTypeParameterSymbol -> cls.owner.representativeUpperBound.classOrUpperBound()
    else -> null
}