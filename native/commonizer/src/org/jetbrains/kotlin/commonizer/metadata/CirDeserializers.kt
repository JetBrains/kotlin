/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.metadata

import kotlin.metadata.*
import kotlin.metadata.Modality as KmModality
import kotlin.metadata.Visibility as KmVisibility
import kotlin.metadata.ClassKind as KmClassKind
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.compileTimeValue
import kotlinx.metadata.klib.getterAnnotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.Variance

object CirDeserializers {
    private fun annotations(
        hasAnnotations: Boolean,
        typeResolver: CirTypeResolver,
        annotations: () -> List<KmAnnotation>,
    ): List<CirAnnotation> {
        return if (!hasAnnotations)
            emptyList()
        else
            annotations().compactMap { annotation(it, typeResolver) }
    }

    private fun annotation(source: KmAnnotation, typeResolver: CirTypeResolver): CirAnnotation {
        val classId = CirEntityId.create(source.className)
        val clazz: CirProvided.RegularClass = typeResolver.resolveClassifier(classId)

        val type = CirClassType.createInterned(
            classId = classId,
            outerType = null, // annotation class can't be inner class
            arguments = clazz.typeParameters.compactMap { typeParameter ->
                CirRegularTypeProjection(
                    projectionKind = typeParameter.variance,
                    type = CirTypeParameterType.createInterned(
                        index = typeParameter.index,
                        isMarkedNullable = false
                    )
                )
            },
            isMarkedNullable = false
        )

        val allValueArguments: Map<String, KmAnnotationArgument> = source.arguments
        if (allValueArguments.isEmpty())
            return CirAnnotation.createInterned(type = type, constantValueArguments = emptyMap(), annotationValueArguments = emptyMap())

        val constantValueArguments: MutableMap<CirName, CirConstantValue> = CommonizerMap(allValueArguments.size)
        val annotationValueArguments: MutableMap<CirName, CirAnnotation> = CommonizerMap(allValueArguments.size)

        allValueArguments.forEach { (name, constantValue) ->
            val cirName = CirName.create(name)
            if (constantValue is KmAnnotationArgument.AnnotationValue)
                annotationValueArguments[cirName] = annotation(source = constantValue.annotation, typeResolver)
            else
                constantValueArguments[cirName] = constantValue(
                    constantValue = constantValue,
                    constantName = cirName,
                    owner = source,
                )
        }

        return CirAnnotation.createInterned(
            type = type,
            constantValueArguments = constantValueArguments.compact(),
            annotationValueArguments = annotationValueArguments.compact()
        )
    }

    private fun typeParameter(source: KmTypeParameter, typeResolver: CirTypeResolver): CirTypeParameter = CirTypeParameter(
        annotations = annotations(true, typeResolver, source::annotations),
        name = CirName.create(source.name),
        isReified = source.isReified,
        variance = variance(source.variance),
        upperBounds = source.filteredUpperBounds.compactMap { type(it, typeResolver) }
    )

    private fun extensionReceiver(
        receiverParameterType: KmType,
        typeResolver: CirTypeResolver
    ): CirExtensionReceiver = CirExtensionReceiver(
        annotations = emptyList(), // TODO nowhere to read receiver annotations from, see KT-42490
        type = type(receiverParameterType, typeResolver)
    )

    fun property(name: CirName, source: KmProperty, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirProperty {
        val compileTimeInitializer = if (source.hasConstant) {
            constantValue(
                constantValue = source.compileTimeValue,
                owner = source,
            )
        } else CirConstantValue.NullValue

        return CirProperty(
            annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.visibility),
            modality = modality(source.modality),
            containingClass = containingClass,
            extensionReceiver = source.receiverParameterType?.let { extensionReceiver(it, typeResolver) },
            returnType = type(source.returnType, typeResolver),
            kind = callableKind(source.kind),
            isVar = source.isVar,
            isLateInit = source.isLateinit,
            isConst = source.isConst,
            isDelegate = source.isDelegated,
            getter = propertyGetter(source, typeResolver),
            setter = propertySetter(source, typeResolver),
            backingFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            delegateFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            compileTimeInitializer = compileTimeInitializer
        )
    }

    private fun propertyGetter(source: KmProperty, typeResolver: CirTypeResolver): CirPropertyGetter? {
        val isDefault = !source.getter.isNotDefault
        val annotations = annotations(source.getter.hasAnnotations, typeResolver, source::getterAnnotations)

        if (isDefault && annotations.isEmpty())
            return CirPropertyGetter.DEFAULT_NO_ANNOTATIONS

        return CirPropertyGetter.createInterned(
            annotations = annotations,
            isDefault = isDefault,
            isInline = source.getter.isInline
        )
    }

    private fun propertySetter(source: KmProperty, typeResolver: CirTypeResolver): CirPropertySetter? {
        val setter = source.setter ?: return null

        return CirPropertySetter.createInterned(
            annotations = annotations(source.setter?.hasAnnotations == true, typeResolver, source::setterAnnotations),
            parameterAnnotations = source.setterParameter?.let { setterParameter ->
                annotations(setterParameter.hasAnnotations, typeResolver, setterParameter::annotations)
            }.orEmpty(),
            visibility = visibility(setter.visibility),
            isDefault = !setter.isNotDefault,
            isInline = setter.isInline
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun callableKind(memberKind: MemberKind): CallableMemberDescriptor.Kind =
        when (memberKind) {
            MemberKind.DECLARATION -> CallableMemberDescriptor.Kind.DECLARATION
            MemberKind.FAKE_OVERRIDE -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            MemberKind.DELEGATION -> CallableMemberDescriptor.Kind.DELEGATION
            MemberKind.SYNTHESIZED -> CallableMemberDescriptor.Kind.SYNTHESIZED
        }

    fun function(name: CirName, source: KmFunction, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirFunction =
        CirFunction(
            annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.visibility),
            modality = modality(source.modality),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { valueParameter(it, typeResolver) },
            hasStableParameterNames = !source.hasNonStableParameterNames,
            extensionReceiver = source.receiverParameterType?.let { extensionReceiver(it, typeResolver) },
            returnType = type(source.returnType, typeResolver),
            kind = callableKind(source.kind),
            modifiers = functionModifiers(source),
        )

    private fun functionModifiers(source: KmFunction): CirFunctionModifiers = CirFunctionModifiers.createInterned(
        isOperator = source.isOperator,
        isInfix = source.isInfix,
        isInline = source.isInline,
        isSuspend = source.isSuspend,
    )

    private fun valueParameter(source: KmValueParameter, typeResolver: CirTypeResolver): CirValueParameter =
        CirValueParameter.createInterned(
            annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
            name = CirName.create(source.name),
            returnType = type(source.type, typeResolver),
            varargElementType = source.varargElementType?.let { type(it, typeResolver) },
            declaresDefaultValue = source.declaresDefaultValue,
            isCrossinline = source.isCrossinline,
            isNoinline = source.isNoinline
        )

    private fun constantValue(
        constantValue: KmAnnotationArgument?,
        constantName: CirName? = null,
        owner: Any,
    ): CirConstantValue = constantValue(
        constantValue = constantValue,
        location = { "${owner::class.java}, $owner" + constantName?.toString()?.let { "[$it]" } }
    )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun constantValue(
        constantValue: KmAnnotationArgument?,
        location: () -> String
    ): CirConstantValue = when (constantValue) {
        null -> CirConstantValue.NullValue

        is KmAnnotationArgument.StringValue -> CirConstantValue.StringValue(constantValue.value)
        is KmAnnotationArgument.CharValue -> CirConstantValue.CharValue(constantValue.value)

        is KmAnnotationArgument.ByteValue -> CirConstantValue.ByteValue(constantValue.value)
        is KmAnnotationArgument.ShortValue -> CirConstantValue.ShortValue(constantValue.value)
        is KmAnnotationArgument.IntValue -> CirConstantValue.IntValue(constantValue.value)
        is KmAnnotationArgument.LongValue -> CirConstantValue.LongValue(constantValue.value)

        is KmAnnotationArgument.UByteValue -> CirConstantValue.UByteValue(constantValue.value)
        is KmAnnotationArgument.UShortValue -> CirConstantValue.UShortValue(constantValue.value)
        is KmAnnotationArgument.UIntValue -> CirConstantValue.UIntValue(constantValue.value)
        is KmAnnotationArgument.ULongValue -> CirConstantValue.ULongValue(constantValue.value)

        is KmAnnotationArgument.FloatValue -> CirConstantValue.FloatValue(constantValue.value)
        is KmAnnotationArgument.DoubleValue -> CirConstantValue.DoubleValue(constantValue.value)
        is KmAnnotationArgument.BooleanValue -> CirConstantValue.BooleanValue(constantValue.value)

        is KmAnnotationArgument.EnumValue -> CirConstantValue.EnumValue(
            CirEntityId.create(constantValue.enumClassName),
            CirName.create(constantValue.enumEntryName)
        )

        is KmAnnotationArgument.ArrayValue -> CirConstantValue.ArrayValue(
            constantValue.elements.compactMapIndexed { index, innerConstantValue ->
                constantValue(
                    constantValue = innerConstantValue,
                    location = { "${location()}[$index]" }
                )
            }
        )

        else -> error("Unsupported annotation argument type: ${constantValue::class.java}, $constantValue")
    }

    fun clazz(name: CirName, source: KmClass, typeResolver: CirTypeResolver): CirClass = CirClass.create(
        annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
        name = name,
        typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
        supertypes = source.filteredSupertypes.compactMap { type(it, typeResolver) },
        visibility = visibility(source.visibility),
        modality = modality(source.modality),
        kind = classKind(source.kind),
        companion = source.companionObject?.let(CirName::create),
        isCompanion = source.kind == KmClassKind.COMPANION_OBJECT,
        isData = source.isData,
        isValue = source.isValue,
        isInner = source.isInner,
        hasEnumEntries = source.hasEnumEntries
    )

    fun defaultEnumEntry(
        name: CirName,
        annotations: List<KmAnnotation>,
        enumClassId: CirEntityId,
        hasEnumEntries: Boolean,
        typeResolver: CirTypeResolver
    ): CirClass = CirClass.create(
        annotations = annotations.compactMap { annotation(it, typeResolver) },
        name = name,
        typeParameters = emptyList(),
        supertypes = listOf(
            CirClassType.createInterned(
                classId = enumClassId,
                outerType = null,
                arguments = emptyList(),
                isMarkedNullable = false
            )
        ),
        visibility = Visibilities.Public,
        modality = Modality.FINAL,
        kind = ClassKind.ENUM_ENTRY,
        companion = null,
        isCompanion = false,
        isData = false,
        isValue = false,
        isInner = false,
        hasEnumEntries = hasEnumEntries
    )

    @Suppress("NOTHING_TO_INLINE")
    private inline fun classKind(kmClassKind: KmClassKind): ClassKind =
        when (kmClassKind) {
            KmClassKind.CLASS -> ClassKind.CLASS
            KmClassKind.INTERFACE -> ClassKind.INTERFACE
            KmClassKind.ENUM_CLASS -> ClassKind.ENUM_CLASS
            KmClassKind.ENUM_ENTRY -> ClassKind.ENUM_ENTRY
            KmClassKind.ANNOTATION_CLASS -> ClassKind.ANNOTATION_CLASS
            KmClassKind.OBJECT, KmClassKind.COMPANION_OBJECT -> ClassKind.OBJECT
        }

    fun constructor(source: KmConstructor, containingClass: CirContainingClass, typeResolver: CirTypeResolver): CirClassConstructor =
        CirClassConstructor.create(
            annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
            typeParameters = emptyList(), // TODO: nowhere to read constructor type parameters from
            visibility = visibility(source.visibility),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { valueParameter(it, typeResolver) },
            hasStableParameterNames = !source.hasNonStableParameterNames,
            isPrimary = !source.isSecondary
        )

    fun typeAlias(name: CirName, source: KmTypeAlias, typeResolver: CirTypeResolver): CirTypeAlias {
        val underlyingType = type(source.underlyingType, typeResolver) as CirClassOrTypeAliasType
        val expandedType = underlyingType.unabbreviate()

        return CirTypeAlias.create(
            annotations = annotations(source.hasAnnotations, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.visibility),
            underlyingType = underlyingType,
            expandedType = expandedType
        )
    }

    private fun type(source: KmType, typeResolver: CirTypeResolver): CirType {
        @Suppress("NAME_SHADOWING")
        val source = source.abbreviatedType ?: source
        val isMarkedNullable = source.isNullable

        return when (val classifier = source.classifier) {
            is KmClassifier.Class -> {
                val classId = CirEntityId.create(classifier.name)

                val outerType = source.outerType?.let { outerType ->
                    val outerClassType = type(outerType, typeResolver)
                    check(outerClassType is CirClassType) { "Outer type of $classId is not a class: $outerClassType" }
                    outerClassType
                }

                val clazz: CirProvided.Class = typeResolver.resolveClassifier(classId)

                CirClassType.createInterned(
                    classId = (clazz as? CirProvided.ExportedForwardDeclarationClass)?.syntheticClassId ?: classId,
                    outerType = outerType,
                    arguments = arguments(source.arguments, typeResolver),
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeAlias -> {
                val typeAliasId = CirEntityId.create(classifier.name)

                val arguments = arguments(source.arguments, typeResolver)

                val underlyingType = CirTypeAliasExpander.expand(
                    CirTypeAliasExpansion.create(typeAliasId, arguments, isMarkedNullable, typeResolver)
                )

                CirTypeAliasType.createInterned(
                    typeAliasId = typeAliasId,
                    underlyingType = underlyingType,
                    arguments = arguments,
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeParameter -> {
                CirTypeParameterType.createInterned(
                    index = typeResolver.resolveTypeParameterIndex(classifier.id),
                    isMarkedNullable = isMarkedNullable
                )
            }
            else -> error("Unexpected classifier type: $classifier")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun variance(variance: KmVariance): Variance = when (variance) {
        KmVariance.INVARIANT -> Variance.INVARIANT
        KmVariance.IN -> Variance.IN_VARIANCE
        KmVariance.OUT -> Variance.OUT_VARIANCE
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun arguments(arguments: List<KmTypeProjection>, typeResolver: CirTypeResolver): List<CirTypeProjection> {
        return arguments.compactMap { argument ->
            val variance = argument.variance ?: return@compactMap CirStarTypeProjection
            val argumentType = argument.type ?: return@compactMap CirStarTypeProjection

            CirRegularTypeProjection(
                projectionKind = variance(variance),
                type = type(argumentType, typeResolver)
            )
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun modality(kmModality: KmModality): Modality =
        when (kmModality) {
            KmModality.FINAL -> Modality.FINAL
            KmModality.ABSTRACT -> Modality.ABSTRACT
            KmModality.OPEN -> Modality.OPEN
            KmModality.SEALED -> Modality.SEALED
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun visibility(kmVisibility: KmVisibility): Visibility =
        when (kmVisibility) {
            KmVisibility.PUBLIC -> Visibilities.Public
            KmVisibility.PROTECTED -> Visibilities.Protected
            KmVisibility.INTERNAL -> Visibilities.Internal
            KmVisibility.PRIVATE -> Visibilities.Private
            else -> error("Can't decode visibility $kmVisibility")
        }
}

