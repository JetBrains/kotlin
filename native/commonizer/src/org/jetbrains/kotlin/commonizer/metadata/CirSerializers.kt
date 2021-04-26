/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.metadata.TypeAliasExpansion.*
import org.jetbrains.kotlin.commonizer.utils.DEFAULT_SETTER_VALUE_NAME
import org.jetbrains.kotlin.commonizer.utils.SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_NAMES
import org.jetbrains.kotlin.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.types.Variance

internal fun CirModule.serializeModule(
    fragments: Collection<KmModuleFragment>
): KlibModuleMetadata = KlibModuleMetadata(
    name = name.toStrippedString(),
    fragments = fragments.toList(),
    annotations = emptyList()
)

internal fun CirPackage.serializePackage(
    allClasses: Collection<KmClass>,
    topLevelTypeAliases: Collection<KmTypeAlias>,
    topLevelFunctions: Collection<KmFunction>,
    topLevelProperties: Collection<KmProperty>
): KmModuleFragment = KmModuleFragment().also { fragment ->
    fragment.fqName = packageName.toString()
    allClasses.forEach {
        fragment.classes += it
        fragment.className += it.name
    }

    if (topLevelTypeAliases.isNotEmpty() || topLevelFunctions.isNotEmpty() || topLevelProperties.isNotEmpty()) {
        fragment.pkg = KmPackage().also { pkg ->
            pkg.fqName = packageName.toString()
            pkg.typeAliases += topLevelTypeAliases
            pkg.functions += topLevelFunctions
            pkg.properties += topLevelProperties
        }
    }
}

internal fun addEmptyFragments(fragments: MutableCollection<KmModuleFragment>) {
    val existingPackageFqNames: Set<String> = fragments.mapTo(HashSet()) { it.fqName!! }

    val missingPackageFqNames: Set<String> = existingPackageFqNames.flatMapTo(HashSet()) { fqName ->
        fqName.mapIndexedNotNull { index, ch ->
            if (ch == '.') {
                val parentFqName = fqName.substring(0, index)
                if (parentFqName !in existingPackageFqNames)
                    return@mapIndexedNotNull parentFqName
            }

            null
        }
    }

    missingPackageFqNames.forEach { fqName ->
        fragments += KmModuleFragment().also { fragment ->
            fragment.fqName = fqName
        }
    }
}

internal fun CirClass.serializeClass(
    context: CirTreeSerializationContext,
    className: ClassName,
    directNestedClasses: Collection<KmClass>,
    nestedConstructors: Collection<KmConstructor>,
    nestedFunctions: Collection<KmFunction>,
    nestedProperties: Collection<KmProperty>
): KmClass = KmClass().also { clazz ->
    clazz.flags = classFlags(isExpect = context.isCommon)
    annotations.mapTo(clazz.annotations) { it.serializeAnnotation() }
    typeParameters.serializeTypeParameters(context, output = clazz.typeParameters)
    clazz.name = className

    clazz.constructors += nestedConstructors
    clazz.functions += nestedFunctions
    clazz.properties += nestedProperties

    directNestedClasses.forEach { directNestedClass ->
        val shortClassName = directNestedClass.name.substringAfterLast('.')

        if (Flag.Class.IS_ENUM_ENTRY(directNestedClass.flags)) {
            clazz.enumEntries += shortClassName
            clazz.klibEnumEntries += KlibEnumEntry(name = shortClassName, annotations = directNestedClass.annotations)
        } else {
            clazz.nestedClasses += shortClassName
        }
    }

    clazz.companionObject = companion?.name

    val supertypes = supertypes
    if (supertypes.isEmpty() && className !in SPECIAL_CLASS_WITHOUT_SUPERTYPES_CLASS_NAMES)
        clazz.supertypes += CirStandardTypes.ANY.serializeClassType(context)
    else
        supertypes.mapTo(clazz.supertypes) { it.serializeType(context) }
}

internal fun linkSealedClassesWithSubclasses(packageName: CirPackageName, classConsumer: ClassConsumer) {
    if (classConsumer.allClasses.isEmpty() || classConsumer.sealedClasses.isEmpty()) return

    val metadataPackageName = packageName.toMetadataString()
    fun ClassName.isInSamePackage(): Boolean = substringBeforeLast('/', "") == metadataPackageName

    val sealedClassesMap: Map<ClassName, KmClass> = classConsumer.sealedClasses.associateBy { it.name }

    classConsumer.allClasses.forEach { clazz ->
        clazz.supertypes.forEach supertype@{ supertype ->
            val superclassName = (supertype.classifier as? KmClassifier.Class)?.name ?: return@supertype
            if (!superclassName.isInSamePackage()) return@supertype
            val sealedClass = sealedClassesMap[superclassName] ?: return@supertype
            sealedClass.sealedSubclasses += clazz.name
        }
    }
}

internal fun CirClassConstructor.serializeConstructor(
    context: CirTreeSerializationContext
): KmConstructor = KmConstructor(
    flags = classConstructorFlags()
).also { constructor ->
    annotations.mapTo(constructor.annotations) { it.serializeAnnotation() }
    // TODO: nowhere to write constructor type parameters
    valueParameters.mapTo(constructor.valueParameters) { it.serializeValueParameter(context) }
}

internal fun CirTypeAlias.serializeTypeAlias(
    context: CirTreeSerializationContext
): KmTypeAlias = KmTypeAlias(
    flags = typeAliasFlags(),
    name = name.name
).also { typeAlias ->
    annotations.mapTo(typeAlias.annotations) { it.serializeAnnotation() }
    typeParameters.serializeTypeParameters(context, output = typeAlias.typeParameters)
    typeAlias.underlyingType = underlyingType.serializeType(context, expansion = ONLY_ABBREVIATIONS)
    typeAlias.expandedType = underlyingType.serializeType(context, expansion = FOR_TOP_LEVEL_TYPE)
}

internal fun CirProperty.serializeProperty(
    context: CirTreeSerializationContext,
): KmProperty = KmProperty(
    flags = propertyFlags(isExpect = context.isCommon && !isLiftedUp),
    name = name.name,
    getterFlags = getter?.propertyAccessorFlags(this, this) ?: NO_FLAGS,
    setterFlags = setter?.let { setter -> setter.propertyAccessorFlags(setter, this) } ?: NO_FLAGS
).also { property ->
    annotations.mapTo(property.annotations) { it.serializeAnnotation() }
    getter?.annotations?.mapTo(property.getterAnnotations) { it.serializeAnnotation() }
    setter?.annotations?.mapTo(property.setterAnnotations) { it.serializeAnnotation() }
    // TODO unclear where to write backing/delegate field annotations, see KT-44625
    property.compileTimeValue = compileTimeInitializer.takeIf { it !is CirConstantValue.NullValue }?.serializeConstantValue()
    typeParameters.serializeTypeParameters(context, output = property.typeParameters)
    extensionReceiver?.let { receiver ->
        // TODO nowhere to write receiver annotations, see KT-42490
        property.receiverParameterType = receiver.type.serializeType(context)
    }
    setter?.takeIf { !it.isDefault }?.let { setter ->
        property.setterParameter = object : CirValueParameter {
            override val annotations get() = setter.parameterAnnotations
            override val name get() = DEFAULT_SETTER_VALUE_NAME
            override val returnType get() = this@serializeProperty.returnType
            override val varargElementType: CirType? get() = null
            override val declaresDefaultValue get() = false
            override val isCrossinline get() = false
            override val isNoinline get() = false
        }.serializeValueParameter(context)
    }
    property.returnType = returnType.serializeType(context)
}

internal fun CirFunction.serializeFunction(
    context: CirTreeSerializationContext,
): KmFunction = KmFunction(
    flags = functionFlags(isExpect = context.isCommon && kind != CallableMemberDescriptor.Kind.SYNTHESIZED),
    name = name.name
).also { function ->
    annotations.mapTo(function.annotations) { it.serializeAnnotation() }
    typeParameters.serializeTypeParameters(context, output = function.typeParameters)
    valueParameters.mapTo(function.valueParameters) { it.serializeValueParameter(context) }
    extensionReceiver?.let { receiver ->
        // TODO nowhere to write receiver annotations, see KT-42490
        function.receiverParameterType = receiver.type.serializeType(context)
    }
    function.returnType = returnType.serializeType(context)
}

private fun CirAnnotation.serializeAnnotation(): KmAnnotation {
    val arguments = LinkedHashMap<String, KmAnnotationArgument>(constantValueArguments.size + annotationValueArguments.size, 1F)

    constantValueArguments.forEach { (name: CirName, value: CirConstantValue) ->
        arguments[name.name] = value.serializeConstantValue()
            ?: error("Unexpected <null> constant value inside of $this")
    }

    annotationValueArguments.forEach { (name: CirName, nested: CirAnnotation) ->
        arguments[name.name] = KmAnnotationArgument.AnnotationValue(nested.serializeAnnotation())
    }

    return KmAnnotation(
        className = type.classifierId.toString(),
        arguments = arguments
    )
}

@OptIn(ExperimentalUnsignedTypes::class)
private fun CirConstantValue.serializeConstantValue(): KmAnnotationArgument? = when (this) {
    is CirConstantValue.StringValue -> KmAnnotationArgument.StringValue(value)
    is CirConstantValue.CharValue -> KmAnnotationArgument.CharValue(value)

    is CirConstantValue.ByteValue -> KmAnnotationArgument.ByteValue(value)
    is CirConstantValue.ShortValue -> KmAnnotationArgument.ShortValue(value)
    is CirConstantValue.IntValue -> KmAnnotationArgument.IntValue(value)
    is CirConstantValue.LongValue -> KmAnnotationArgument.LongValue(value)

    is CirConstantValue.UByteValue -> KmAnnotationArgument.UByteValue(value)
    is CirConstantValue.UShortValue -> KmAnnotationArgument.UShortValue(value)
    is CirConstantValue.UIntValue -> KmAnnotationArgument.UIntValue(value)
    is CirConstantValue.ULongValue -> KmAnnotationArgument.ULongValue(value)

    is CirConstantValue.FloatValue -> KmAnnotationArgument.FloatValue(value)
    is CirConstantValue.DoubleValue -> KmAnnotationArgument.DoubleValue(value)
    is CirConstantValue.BooleanValue -> KmAnnotationArgument.BooleanValue(value)

    is CirConstantValue.EnumValue -> KmAnnotationArgument.EnumValue(enumClassId.toString(), enumEntryName.name)
    is CirConstantValue.NullValue -> null

    is CirConstantValue.ArrayValue -> KmAnnotationArgument.ArrayValue(elements.compactMap { element ->
        element.serializeConstantValue() ?: error("Unexpected <null> constant value inside of $this")
    })
}

private fun CirValueParameter.serializeValueParameter(
    context: CirTreeSerializationContext
): KmValueParameter = KmValueParameter(
    flags = valueParameterFlags(),
    name = name.name
).also { parameter ->
    annotations.mapTo(parameter.annotations) { it.serializeAnnotation() }
    parameter.type = returnType.serializeType(context)
    varargElementType?.let { varargElementType ->
        parameter.varargElementType = varargElementType.serializeType(context)
    }
}

private fun List<CirTypeParameter>.serializeTypeParameters(
    context: CirTreeSerializationContext,
    output: MutableList<KmTypeParameter>
) {
    mapIndexedTo(output) { index, cirTypeParameter ->
        KmTypeParameter(
            flags = cirTypeParameter.typeParameterFlags(),
            name = cirTypeParameter.name.name,
            id = context.typeParameterIndexOffset + index,
            variance = cirTypeParameter.variance.serializeVariance()
        ).also { parameter ->
            cirTypeParameter.upperBounds.mapTo(parameter.upperBounds) { it.serializeType(context) }
            cirTypeParameter.annotations.mapTo(parameter.annotations) { it.serializeAnnotation() }
        }
    }
}

private fun CirType.serializeType(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion = FOR_TOP_LEVEL_TYPE
): KmType = when (this) {
    is CirClassType -> serializeClassType(context, expansion)
    is CirTypeAliasType -> serializeTypeAliasType(context, expansion)
    is CirTypeParameterType -> serializeTypeParameterType()
    is CirFlexibleType -> {
        lowerBound.serializeType(context, expansion).also {
            it.flexibleTypeUpperBound = KmFlexibleTypeUpperBound(
                type = upperBound.serializeType(context, expansion),
                typeFlexibilityId = DynamicTypeDeserializer.id
            )
        }
    }
}

private fun CirTypeParameterType.serializeTypeParameterType(): KmType =
    KmType(typeFlags()).also { type ->
        type.classifier = KmClassifier.TypeParameter(index)
    }

private fun CirClassType.serializeClassType(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion = FOR_TOP_LEVEL_TYPE
): KmType = KmType(typeFlags()).also { type ->
    type.classifier = KmClassifier.Class(classifierId.toString())
    arguments.mapTo(type.arguments) { it.serializeArgument(context, expansion) }
    outerType?.let { type.outerType = it.serializeClassType(context, expansion) }
}

private fun CirTypeAliasType.serializeTypeAliasType(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion
): KmType = when (expansion) {
    ONLY_ABBREVIATIONS -> serializeAbbreviationType(context, expansion)
    EXPANDED_WITHOUT_ABBREVIATIONS -> serializeExpandedType(context, expansion)
    FOR_TOP_LEVEL_TYPE -> serializeExpandedType(context, EXPANDED_WITHOUT_ABBREVIATIONS).apply {
        abbreviatedType = serializeAbbreviationType(context, expansion)
    }
    FOR_NESTED_TYPE -> serializeExpandedType(context, expansion).apply {
        abbreviatedType = serializeAbbreviationType(context, expansion)
    }
}

private fun CirTypeAliasType.serializeAbbreviationType(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion
): KmType {
    val abbreviationType = KmType(typeFlags())
    abbreviationType.classifier = KmClassifier.TypeAlias(classifierId.toString())
    arguments.mapTo(abbreviationType.arguments) { it.serializeArgument(context, expansion) }
    return abbreviationType
}

@Suppress("UnnecessaryVariable")
private fun CirTypeAliasType.serializeExpandedType(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion
): KmType {
    val cirExpandedType = computeExpandedType(underlyingType)
    val expandedType = cirExpandedType.serializeClassType(context, expansion)
    return expandedType
}

private fun CirTypeProjection.serializeArgument(
    context: CirTreeSerializationContext,
    expansion: TypeAliasExpansion
): KmTypeProjection {
    val effectiveExpansion = if (expansion == FOR_TOP_LEVEL_TYPE) FOR_NESTED_TYPE else expansion
    return when (this) {
        CirStarTypeProjection -> KmTypeProjection.STAR
        is CirRegularTypeProjection -> KmTypeProjection(
            variance = projectionKind.serializeVariance(),
            type = type.serializeType(context, effectiveExpansion)
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Variance.serializeVariance(): KmVariance = when (this) {
    Variance.INVARIANT -> KmVariance.INVARIANT
    Variance.IN_VARIANCE -> KmVariance.IN
    Variance.OUT_VARIANCE -> KmVariance.OUT
}

@Suppress("SpellCheckingInspection")
private enum class TypeAliasExpansion {
    ONLY_ABBREVIATIONS,
    EXPANDED_WITHOUT_ABBREVIATIONS,
    FOR_TOP_LEVEL_TYPE,
    FOR_NESTED_TYPE
}
