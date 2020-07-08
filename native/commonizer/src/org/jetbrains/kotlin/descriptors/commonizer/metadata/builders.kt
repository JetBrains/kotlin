/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.metadata

import kotlinx.metadata.*
import kotlinx.metadata.klib.*
import org.jetbrains.kotlin.backend.common.serialization.metadata.DynamicTypeDeserializer
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirValueParameterImpl
import org.jetbrains.kotlin.descriptors.commonizer.core.computeExpandedType
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.commonizer.metadata.TypeAliasExpansion.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.strip
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.types.Variance

internal fun CirModule.buildModule(
    fragments: Collection<KmModuleFragment>
): KlibModuleMetadata = KlibModuleMetadata(
    name = name.strip(),
    fragments = fragments.toList(),
    annotations = emptyList()
)

internal fun CirPackage.buildModuleFragment(
    allClasses: Collection<KmClass>,
    topLevelTypeAliases: Collection<KmTypeAlias>,
    topLevelFunctions: Collection<KmFunction>,
    topLevelProperties: Collection<KmProperty>
): KmModuleFragment = KmModuleFragment().also { fragment ->
    fragment.fqName = fqName.asString()
    allClasses.forEach {
        fragment.classes += it
        fragment.className += it.name
    }

    if (topLevelTypeAliases.isNotEmpty() || topLevelFunctions.isNotEmpty() || topLevelProperties.isNotEmpty()) {
        fragment.pkg = KmPackage().also { pkg ->
            pkg.fqName = fqName.asString()
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

internal fun CirClass.buildClass(
    context: VisitingContext,
    className: ClassName,
    directNestedClasses: Collection<KmClass>,
    nestedConstructors: Collection<KmConstructor>,
    nestedFunctions: Collection<KmFunction>,
    nestedProperties: Collection<KmProperty>
): KmClass = KmClass().also { clazz ->
    clazz.flags = classFlags(isExpect = context.isCommon)
    annotations.mapTo(clazz.annotations) { it.buildAnnotation() }
    typeParameters.buildTypeParameters(context, output = clazz.typeParameters)
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

    clazz.companionObject = companion?.asString()
    supertypes.mapTo(clazz.supertypes) { it.buildType(context) }
}

internal fun linkSealedClassesWithSubclasses(packageFqName: FqName, classConsumer: ClassConsumer) {
    if (classConsumer.allClasses.isEmpty() || classConsumer.sealedClasses.isEmpty()) return

    val packageName = packageFqName.asString().replace('.', '/')
    fun ClassName.isInSamePackage(): Boolean = substringBeforeLast('/', "") == packageName

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

internal fun CirClassConstructor.buildClassConstructor(
    context: VisitingContext
): KmConstructor = KmConstructor(
    flags = classConstructorFlags()
).also { constructor ->
    annotations.mapTo(constructor.annotations) { it.buildAnnotation() }
    valueParameters.mapTo(constructor.valueParameters) { it.buildValueParameter(context) }
}

internal fun CirTypeAlias.buildTypeAlias(
    context: VisitingContext
): KmTypeAlias = KmTypeAlias(
    flags = typeAliasFlags(),
    name = name.asString()
).also { typeAlias ->
    annotations.mapTo(typeAlias.annotations) { it.buildAnnotation() }
    typeParameters.buildTypeParameters(context, output = typeAlias.typeParameters)
    typeAlias.underlyingType = underlyingType.buildType(context, expansion = ONLY_ABBREVIATIONS)
    typeAlias.expandedType = underlyingType.buildType(context, expansion = FOR_TOP_LEVEL_TYPE)
}

internal fun CirProperty.buildProperty(
    context: VisitingContext,
): KmProperty = KmProperty(
    flags = propertyFlags(isExpect = context.isCommon && !isLiftedUp),
    name = name.asString(),
    getterFlags = getter?.propertyAccessorFlags(this, this) ?: NO_FLAGS,
    setterFlags = setter?.let { setter -> setter.propertyAccessorFlags(setter, this) } ?: NO_FLAGS
).also { property ->
    annotations.mapTo(property.annotations) { it.buildAnnotation() }
    getter?.annotations?.mapTo(property.getterAnnotations) { it.buildAnnotation() }
    setter?.annotations?.mapTo(property.setterAnnotations) { it.buildAnnotation() }
    property.compileTimeValue = compileTimeInitializer?.takeIf { it !is NullValue }?.buildAnnotationArgument()
    typeParameters.buildTypeParameters(context, output = property.typeParameters)
    extensionReceiver?.let { receiver ->
        // TODO nowhere to write receiver annotations, see KT-42490
        property.receiverParameterType = receiver.type.buildType(context)
    }
    setter?.takeIf { !it.isDefault }?.let { setter ->
        property.setterParameter = CirValueParameterImpl(
            annotations = setter.parameterAnnotations,
            name = SETTER_VALUE_NAME,
            returnType = returnType,
            varargElementType = null,
            declaresDefaultValue = false,
            isCrossinline = false,
            isNoinline = false
        ).buildValueParameter(context)
    }
    property.returnType = returnType.buildType(context)
}

internal fun CirFunction.buildFunction(
    context: VisitingContext,
): KmFunction = KmFunction(
    flags = functionFlags(isExpect = context.isCommon),
    name = name.asString()
).also { function ->
    annotations.mapTo(function.annotations) { it.buildAnnotation() }
    typeParameters.buildTypeParameters(context, output = function.typeParameters)
    valueParameters.mapTo(function.valueParameters) { it.buildValueParameter(context) }
    extensionReceiver?.let { receiver ->
        // TODO nowhere to write receiver annotations, see KT-42490
        function.receiverParameterType = receiver.type.buildType(context)
    }
    function.returnType = returnType.buildType(context)
}

private fun CirAnnotation.buildAnnotation(): KmAnnotation {
    val arguments = LinkedHashMap<String, KmAnnotationArgument<*>>(constantValueArguments.size + annotationValueArguments.size, 1F)

    constantValueArguments.forEach { (name: Name, value: ConstantValue<*>) ->
        arguments[name.asString()] = value.buildAnnotationArgument()
    }

    annotationValueArguments.forEach { (name: Name, nested: CirAnnotation) ->
        arguments[name.asString()] = KmAnnotationArgument.AnnotationValue(nested.buildAnnotation())
    }

    return KmAnnotation(
        className = type.classifierId.asString(),
        arguments = arguments
    )
}

private fun ConstantValue<*>.buildAnnotationArgument(): KmAnnotationArgument<*> = when (this) {
    is StringValue -> KmAnnotationArgument.StringValue(value)
    is CharValue -> KmAnnotationArgument.CharValue(value)

    is ByteValue -> KmAnnotationArgument.ByteValue(value)
    is ShortValue -> KmAnnotationArgument.ShortValue(value)
    is IntValue -> KmAnnotationArgument.IntValue(value)
    is LongValue -> KmAnnotationArgument.LongValue(value)

    is UByteValue -> KmAnnotationArgument.UByteValue(value)
    is UShortValue -> KmAnnotationArgument.UShortValue(value)
    is UIntValue -> KmAnnotationArgument.UIntValue(value)
    is ULongValue -> KmAnnotationArgument.ULongValue(value)

    is FloatValue -> KmAnnotationArgument.FloatValue(value)
    is DoubleValue -> KmAnnotationArgument.DoubleValue(value)
    is BooleanValue -> KmAnnotationArgument.BooleanValue(value)

    is EnumValue -> KmAnnotationArgument.EnumValue(enumClassId.asString(), enumEntryName.asString())
    is ArrayValue -> KmAnnotationArgument.ArrayValue(value.map { it.buildAnnotationArgument() })

    else -> error("Unsupported annotation argument type: ${this::class.java}, $this")
}

private fun CirValueParameter.buildValueParameter(
    context: VisitingContext
): KmValueParameter = KmValueParameter(
    flags = valueParameterFlags(),
    name = name.asString()
).also { parameter ->
    annotations.mapTo(parameter.annotations) { it.buildAnnotation() }
    parameter.type = returnType.buildType(context)
    varargElementType?.let { varargElementType ->
        parameter.varargElementType = varargElementType.buildType(context)
    }
}

private fun List<CirTypeParameter>.buildTypeParameters(
    context: VisitingContext,
    output: MutableList<KmTypeParameter>
) {
    mapIndexedTo(output) { index, cirTypeParameter ->
        KmTypeParameter(
            flags = cirTypeParameter.typeParameterFlags(),
            name = cirTypeParameter.name.asString(),
            id = context.typeParameterIndexOffset + index,
            variance = cirTypeParameter.variance.buildVariance()
        ).also { parameter ->
            cirTypeParameter.upperBounds.mapTo(parameter.upperBounds) { it.buildType(context) }
            cirTypeParameter.annotations.mapTo(parameter.annotations) { it.buildAnnotation() }
        }
    }
}

private fun CirType.buildType(
    context: VisitingContext,
    expansion: TypeAliasExpansion = FOR_TOP_LEVEL_TYPE
): KmType = when (this) {
    is CirClassType -> buildType(context, expansion)
    is CirTypeAliasType -> buildType(context, expansion)
    is CirTypeParameterType -> buildType()
    is CirFlexibleType -> {
        lowerBound.buildType(context, expansion).also {
            it.flexibleTypeUpperBound = KmFlexibleTypeUpperBound(
                type = upperBound.buildType(context, expansion),
                typeFlexibilityId = DynamicTypeDeserializer.id
            )
        }
    }
}

private fun CirTypeParameterType.buildType(): KmType =
    KmType(typeFlags()).also { type ->
        type.classifier = KmClassifier.TypeParameter(index)
    }

private fun CirClassType.buildType(
    context: VisitingContext,
    expansion: TypeAliasExpansion
): KmType = KmType(typeFlags()).also { type ->
    type.classifier = KmClassifier.Class(classifierId.asString())
    arguments.mapTo(type.arguments) { it.buildArgument(context, expansion) }
    outerType?.let { type.outerType = it.buildType(context, expansion) }
}

private fun CirTypeAliasType.buildType(
    context: VisitingContext,
    expansion: TypeAliasExpansion
): KmType = when (expansion) {
    ONLY_ABBREVIATIONS -> buildAbbreviationType(context, expansion)
    EXPANDED_WITHOUT_ABBREVIATIONS -> buildExpandedType(context, expansion)
    FOR_TOP_LEVEL_TYPE -> buildExpandedType(context, EXPANDED_WITHOUT_ABBREVIATIONS).apply {
        abbreviatedType = buildAbbreviationType(context, expansion)
    }
    FOR_NESTED_TYPE -> buildExpandedType(context, expansion).apply {
        abbreviatedType = buildAbbreviationType(context, expansion)
    }
}

private fun CirTypeAliasType.buildAbbreviationType(
    context: VisitingContext,
    expansion: TypeAliasExpansion
): KmType {
    val abbreviationType = KmType(typeFlags())
    abbreviationType.classifier = KmClassifier.TypeAlias(classifierId.asString())
    arguments.mapTo(abbreviationType.arguments) { it.buildArgument(context, expansion) }
    return abbreviationType
}

@Suppress("UnnecessaryVariable")
private fun CirTypeAliasType.buildExpandedType(
    context: VisitingContext,
    expansion: TypeAliasExpansion
): KmType {
    val cirExpandedType = computeExpandedType(underlyingType)
    val expandedType = cirExpandedType.buildType(context, expansion)
    return expandedType
}

private fun CirTypeProjection.buildArgument(
    context: VisitingContext,
    expansion: TypeAliasExpansion
): KmTypeProjection {
    val effectiveExpansion = if (expansion == FOR_TOP_LEVEL_TYPE) FOR_NESTED_TYPE else expansion
    return when (this) {
        CirStarTypeProjection -> KmTypeProjection.STAR
        is CirTypeProjectionImpl -> KmTypeProjection(
            variance = projectionKind.buildVariance(),
            type = type.buildType(context, effectiveExpansion)
        )
    }
}

@Suppress("NOTHING_TO_INLINE")
private inline fun Variance.buildVariance() = when (this) {
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

private val SETTER_VALUE_NAME = Name.identifier("value")
