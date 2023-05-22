/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.metadata

import gnu.trove.THashMap
import kotlinx.metadata.*
import kotlinx.metadata.klib.annotations
import kotlinx.metadata.klib.compileTimeValue
import kotlinx.metadata.klib.getterAnnotations
import kotlinx.metadata.klib.setterAnnotations
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.types.Variance

object CirDeserializers {
    private fun annotations(flags: Flags, typeResolver: CirTypeResolver, annotations: () -> List<KmAnnotation>): List<CirAnnotation> {
        return if (!Flag.Common.HAS_ANNOTATIONS(flags))
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

        val constantValueArguments: MutableMap<CirName, CirConstantValue> = THashMap(allValueArguments.size)
        val annotationValueArguments: MutableMap<CirName, CirAnnotation> = THashMap(allValueArguments.size)

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

    private val ALWAYS_HAS_ANNOTATIONS: Flags = flagsOf(Flag.Common.HAS_ANNOTATIONS)

    private fun typeParameter(source: KmTypeParameter, typeResolver: CirTypeResolver): CirTypeParameter = CirTypeParameter(
        annotations = annotations(ALWAYS_HAS_ANNOTATIONS, typeResolver, source::annotations),
        name = CirName.create(source.name),
        isReified = Flag.TypeParameter.IS_REIFIED(source.flags),
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
        val compileTimeInitializer = if (Flag.Property.HAS_CONSTANT(source.flags)) {
            constantValue(
                constantValue = source.compileTimeValue,
                owner = source,
            )
        } else CirConstantValue.NullValue

        return CirProperty(
            annotations = annotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.flags),
            modality = modality(source.flags),
            containingClass = containingClass,
            extensionReceiver = source.receiverParameterType?.let { extensionReceiver(it, typeResolver) },
            returnType = type(source.returnType, typeResolver),
            kind = callableKind(source.flags),
            isVar = Flag.Property.IS_VAR(source.flags),
            isLateInit = Flag.Property.IS_LATEINIT(source.flags),
            isConst = Flag.Property.IS_CONST(source.flags),
            isDelegate = Flag.Property.IS_DELEGATED(source.flags),
            getter = propertyGetter(source, typeResolver),
            setter = propertySetter(source, typeResolver),
            backingFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            delegateFieldAnnotations = emptyList(), // TODO unclear where to read backing/delegate field annotations from, see KT-44625
            compileTimeInitializer = compileTimeInitializer
        )
    }

    private fun propertyGetter(source: KmProperty, typeResolver: CirTypeResolver): CirPropertyGetter? {
        if (!Flag.Property.HAS_GETTER(source.flags))
            return null

        val getterFlags = source.getterFlags

        val isDefault = !Flag.PropertyAccessor.IS_NOT_DEFAULT(getterFlags)
        val annotations = annotations(getterFlags, typeResolver, source::getterAnnotations)

        if (isDefault && annotations.isEmpty())
            return CirPropertyGetter.DEFAULT_NO_ANNOTATIONS

        return CirPropertyGetter.createInterned(
            annotations = annotations,
            isDefault = isDefault,
            isInline = Flag.PropertyAccessor.IS_INLINE(getterFlags)
        )
    }

    private fun propertySetter(source: KmProperty, typeResolver: CirTypeResolver): CirPropertySetter? {
        if (!Flag.Property.HAS_SETTER(source.flags))
            return null

        val setterFlags = source.setterFlags

        return CirPropertySetter.createInterned(
            annotations = annotations(setterFlags, typeResolver, source::setterAnnotations),
            parameterAnnotations = source.setterParameter?.let { setterParameter ->
                annotations(setterParameter.flags, typeResolver, setterParameter::annotations)
            }.orEmpty(),
            visibility = visibility(setterFlags),
            isDefault = !Flag.PropertyAccessor.IS_NOT_DEFAULT(setterFlags),
            isInline = Flag.PropertyAccessor.IS_INLINE(setterFlags)
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun callableKind(flags: Flags): CallableMemberDescriptor.Kind =
        when {
            Flag.Property.IS_DECLARATION(flags) /*|| Flag.Function.IS_DECLARATION(flags)*/ -> CallableMemberDescriptor.Kind.DECLARATION
            Flag.Property.IS_FAKE_OVERRIDE(flags) /*|| Flag.Function.IS_FAKE_OVERRIDE(flags)*/ -> CallableMemberDescriptor.Kind.FAKE_OVERRIDE
            Flag.Property.IS_DELEGATION(flags) /*|| Flag.Function.IS_DELEGATION(flags)*/ -> CallableMemberDescriptor.Kind.DELEGATION
            Flag.Property.IS_SYNTHESIZED(flags) /*|| Flag.Function.IS_SYNTHESIZED(flags)*/ -> CallableMemberDescriptor.Kind.SYNTHESIZED
            else -> error("Can't decode callable kind from flags: $flags")
        }

    fun function(name: CirName, source: KmFunction, containingClass: CirContainingClass?, typeResolver: CirTypeResolver): CirFunction =
        CirFunction(
            annotations = annotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.flags),
            modality = modality(source.flags),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { valueParameter(it, typeResolver) },
            hasStableParameterNames = !Flag.Function.HAS_NON_STABLE_PARAMETER_NAMES(source.flags),
            extensionReceiver = source.receiverParameterType?.let { extensionReceiver(it, typeResolver) },
            returnType = type(source.returnType, typeResolver),
            kind = callableKind(source.flags),
            modifiers = functionModifiers(source),
        )

    private fun functionModifiers(source: KmFunction): CirFunctionModifiers = CirFunctionModifiers.createInterned(
        isOperator = Flag.Function.IS_OPERATOR(source.flags),
        isInfix = Flag.Function.IS_INFIX(source.flags),
        isInline = Flag.Function.IS_INLINE(source.flags),
        isSuspend = Flag.Function.IS_SUSPEND(source.flags),
    )

    private fun valueParameter(source: KmValueParameter, typeResolver: CirTypeResolver): CirValueParameter =
        CirValueParameter.createInterned(
            annotations = annotations(source.flags, typeResolver, source::annotations),
            name = CirName.create(source.name),
            returnType = type(source.type, typeResolver),
            varargElementType = source.varargElementType?.let { type(it, typeResolver) },
            declaresDefaultValue = Flag.ValueParameter.DECLARES_DEFAULT_VALUE(source.flags),
            isCrossinline = Flag.ValueParameter.IS_CROSSINLINE(source.flags),
            isNoinline = Flag.ValueParameter.IS_NOINLINE(source.flags)
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
        annotations = annotations(source.flags, typeResolver, source::annotations),
        name = name,
        typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
        supertypes = source.filteredSupertypes.compactMap { type(it, typeResolver) },
        visibility = visibility(source.flags),
        modality = modality(source.flags),
        kind = classKind(source.flags),
        companion = source.companionObject?.let(CirName::create),
        isCompanion = Flag.Class.IS_COMPANION_OBJECT(source.flags),
        isData = Flag.Class.IS_DATA(source.flags),
        isValue = Flag.Class.IS_VALUE(source.flags),
        isInner = Flag.Class.IS_INNER(source.flags),
        hasEnumEntries = Flag.Class.HAS_ENUM_ENTRIES(source.flags)
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
    private inline fun classKind(flags: Flags): ClassKind =
        when {
            Flag.Class.IS_CLASS(flags) -> ClassKind.CLASS
            Flag.Class.IS_INTERFACE(flags) -> ClassKind.INTERFACE
            Flag.Class.IS_ENUM_CLASS(flags) -> ClassKind.ENUM_CLASS
            Flag.Class.IS_ENUM_ENTRY(flags) -> ClassKind.ENUM_ENTRY
            Flag.Class.IS_ANNOTATION_CLASS(flags) -> ClassKind.ANNOTATION_CLASS
            Flag.Class.IS_OBJECT(flags) || Flag.Class.IS_COMPANION_OBJECT(flags) -> ClassKind.OBJECT
            else -> error("Can't decode class kind from flags: $flags")
        }

    fun constructor(source: KmConstructor, containingClass: CirContainingClass, typeResolver: CirTypeResolver): CirClassConstructor =
        CirClassConstructor.create(
            annotations = annotations(source.flags, typeResolver, source::annotations),
            typeParameters = emptyList(), // TODO: nowhere to read constructor type parameters from
            visibility = visibility(source.flags),
            containingClass = containingClass,
            valueParameters = source.valueParameters.compactMap { valueParameter(it, typeResolver) },
            hasStableParameterNames = !Flag.Constructor.HAS_NON_STABLE_PARAMETER_NAMES(source.flags),
            isPrimary = !Flag.Constructor.IS_SECONDARY(source.flags)
        )

    fun typeAlias(name: CirName, source: KmTypeAlias, typeResolver: CirTypeResolver): CirTypeAlias {
        val underlyingType = type(source.underlyingType, typeResolver) as CirClassOrTypeAliasType
        val expandedType = underlyingType.unabbreviate()

        return CirTypeAlias.create(
            annotations = annotations(source.flags, typeResolver, source::annotations),
            name = name,
            typeParameters = source.typeParameters.compactMap { typeParameter(it, typeResolver) },
            visibility = visibility(source.flags),
            underlyingType = underlyingType,
            expandedType = expandedType
        )
    }

    private fun type(source: KmType, typeResolver: CirTypeResolver): CirType {
        @Suppress("NAME_SHADOWING")
        val source = source.abbreviatedType ?: source
        val isMarkedNullable = Flag.Type.IS_NULLABLE(source.flags)

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
    private inline fun modality(flags: Flags): Modality =
        when {
            Flag.Common.IS_FINAL(flags) -> Modality.FINAL
            Flag.Common.IS_ABSTRACT(flags) -> Modality.ABSTRACT
            Flag.Common.IS_OPEN(flags) -> Modality.OPEN
            Flag.Common.IS_SEALED(flags) -> Modality.SEALED
            else -> error("Can't decode modality from flags: $flags")
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun visibility(flags: Flags): Visibility =
        when {
            Flag.Common.IS_PUBLIC(flags) -> Visibilities.Public
            Flag.Common.IS_PROTECTED(flags) -> Visibilities.Protected
            Flag.Common.IS_INTERNAL(flags) -> Visibilities.Internal
            Flag.Common.IS_PRIVATE(flags) -> Visibilities.Private
            else -> error("Can't decode visibility from flags: $flags")
        }
}

