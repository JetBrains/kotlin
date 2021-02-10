/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import gnu.trove.TIntObjectHashMap
import kotlinx.metadata.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirTypeAliasTypeImpl
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirProvidedClassifiers
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.TypeParameterResolver
import org.jetbrains.kotlin.descriptors.commonizer.core.computeExpandedType
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.types.*

object CirTypeFactory {
    object StandardTypes {
        val ANY: CirClassType = createClassType(
            classId = ANY_CLASS_ID,
            outerType = null,
            visibility = DescriptorVisibilities.PUBLIC,
            arguments = emptyList(),
            isMarkedNullable = false
        )
    }

    private val classTypeInterner = Interner<CirClassType>()
    private val typeAliasTypeInterner = Interner<CirTypeAliasType>()
    private val typeParameterTypeInterner = Interner<CirTypeParameterType>()

    fun create(source: KmType, typeResolver: CirTypeResolver): CirType {
        @Suppress("NAME_SHADOWING")
        val source = source.abbreviatedType ?: source
        val isMarkedNullable = Flag.Type.IS_NULLABLE(source.flags)

        return when (val classifier = source.classifier) {
            is KmClassifier.Class -> {
                val classId = CirEntityId.create(classifier.name)

                val outerType = source.outerType?.let { outerType ->
                    val outerClassType = create(outerType, typeResolver)
                    check(outerClassType is CirClassType) { "Outer type of $classId is not a class: $outerClassType" }
                    outerClassType
                }

                val clazz: CirProvided.Class = typeResolver.resolveClassifier(classId)

                createClassType(
                    classId = classId,
                    outerType = outerType,
                    visibility = clazz.visibility,
                    arguments = createArguments(source.arguments, typeResolver),
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeAlias -> {
                val typeAliasId = CirEntityId.create(classifier.name)

                val arguments = createArguments(source.arguments, typeResolver)

                val underlyingType = CirTypeAliasExpander.expand(
                    CirTypeAliasExpansion.create(typeAliasId, arguments, isMarkedNullable, typeResolver)
                )

                createTypeAliasType(
                    typeAliasId = typeAliasId,
                    underlyingType = underlyingType,
                    arguments = arguments,
                    isMarkedNullable = isMarkedNullable
                )
            }
            is KmClassifier.TypeParameter -> {
                createTypeParameterType(
                    index = typeResolver.resolveTypeParameterIndex(classifier.id),
                    isMarkedNullable = isMarkedNullable
                )
            }
        }
    }

    fun create(source: KotlinType): CirType = source.unwrap().run {
        when (this) {
            is SimpleType -> create(this)
            is FlexibleType -> CirFlexibleType(create(lowerBound), create(upperBound))
        }
    }

    fun create(source: SimpleType): CirSimpleType {
        if (source is AbbreviatedType) {
            val abbreviation = source.abbreviation
            when (val classifierDescriptor = abbreviation.declarationDescriptor) {
                is TypeAliasDescriptor -> {
                    return createTypeAliasType(
                        typeAliasId = classifierDescriptor.classifierId,
                        underlyingType = create(extractExpandedType(source)) as CirClassOrTypeAliasType,
                        arguments = createArguments(abbreviation.arguments),
                        isMarkedNullable = abbreviation.isMarkedNullable
                    )
                }
                else -> error("Unexpected classifier descriptor type for abbreviation type: ${classifierDescriptor::class.java}, $classifierDescriptor, ${source.abbreviation}")
            }
        }

        return when (val classifierDescriptor = source.declarationDescriptor) {
            is ClassDescriptor -> createClassTypeWithAllOuterTypes(
                classDescriptor = classifierDescriptor,
                arguments = createArguments(source.arguments),
                isMarkedNullable = source.isMarkedNullable
            )
            is TypeAliasDescriptor -> {
                val abbreviatedType = TypeAliasExpander.NON_REPORTING.expand(
                    TypeAliasExpansion.create(null, classifierDescriptor, source.arguments),
                    Annotations.EMPTY
                ) as AbbreviatedType

                val expandedType = extractExpandedType(abbreviatedType)

                val cirExpandedType = create(expandedType) as CirClassOrTypeAliasType
                val cirExpandedTypeWithProperNullability = makeNullableIfNecessary(cirExpandedType, source.isMarkedNullable)

                createTypeAliasType(
                    typeAliasId = classifierDescriptor.classifierId,
                    underlyingType = cirExpandedTypeWithProperNullability,
                    arguments = createArguments(source.arguments),
                    isMarkedNullable = source.isMarkedNullable
                )
            }
            is TypeParameterDescriptor -> createTypeParameterType(classifierDescriptor.typeParameterIndex, source.isMarkedNullable)
            else -> error("Unexpected classifier descriptor type: ${classifierDescriptor::class.java}, $classifierDescriptor, $source")
        }
    }

    fun createClassType(
        classId: CirEntityId,
        outerType: CirClassType?,
        visibility: DescriptorVisibility,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirClassType {
        return classTypeInterner.intern(
            CirClassTypeImpl(
                classifierId = classId,
                outerType = outerType,
                visibility = visibility,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    fun createTypeAliasType(
        typeAliasId: CirEntityId,
        underlyingType: CirClassOrTypeAliasType,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirTypeAliasType {
        return typeAliasTypeInterner.intern(
            CirTypeAliasTypeImpl(
                classifierId = typeAliasId,
                underlyingType = underlyingType,
                arguments = arguments,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun createTypeParameterType(
        index: Int,
        isMarkedNullable: Boolean
    ): CirTypeParameterType {
        return typeParameterTypeInterner.intern(
            CirTypeParameterType(
                index = index,
                isMarkedNullable = isMarkedNullable
            )
        )
    }

    fun <T : CirSimpleType> makeNullable(type: T): T {
        if (type.isMarkedNullable)
            return type

        val result = when (type) {
            is CirClassType -> createClassType(
                classId = type.classifierId,
                outerType = type.outerType,
                visibility = type.visibility,
                arguments = type.arguments,
                isMarkedNullable = true
            )
            is CirTypeAliasType -> createTypeAliasType(
                typeAliasId = type.classifierId,
                underlyingType = makeNullable(type.underlyingType),
                arguments = type.arguments,
                isMarkedNullable = true
            )
            is CirTypeParameterType -> createTypeParameterType(
                index = type.index,
                isMarkedNullable = true
            )
            else -> error("Unsupported type: $type")
        }

        @Suppress("UNCHECKED_CAST")
        return result as T
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun <T : CirSimpleType> makeNullableIfNecessary(type: T, necessary: Boolean): T =
        if (!necessary) type else makeNullable(type)

    @Suppress("NOTHING_TO_INLINE")
    inline fun decodeVariance(variance: KmVariance): Variance = when (variance) {
        KmVariance.INVARIANT -> Variance.INVARIANT
        KmVariance.IN -> Variance.IN_VARIANCE
        KmVariance.OUT -> Variance.OUT_VARIANCE
    }

    fun unabbreviate(type: CirClassOrTypeAliasType): CirClassType = when (type) {
        is CirClassType -> {
            var hasAbbreviationsInArguments = false
            val unabreviatedArguments = type.arguments.compactMap { argument ->
                val argumentType =
                    (argument as? CirTypeProjectionImpl)?.type as? CirClassOrTypeAliasType ?: return@compactMap argument
                val unabbreviatedArgumentType = unabbreviate(argumentType)

                if (argumentType == unabbreviatedArgumentType)
                    argument
                else {
                    hasAbbreviationsInArguments = true
                    CirTypeProjectionImpl(
                        projectionKind = argument.projectionKind,
                        type = unabbreviatedArgumentType
                    )
                }
            }

            val outerType = type.outerType
            val unabbreviatedOuterType = outerType?.let(::unabbreviate)

            if (!hasAbbreviationsInArguments && outerType == unabbreviatedOuterType)
                type
            else
                createClassType(
                    classId = type.classifierId,
                    outerType = unabbreviatedOuterType,
                    visibility = type.visibility,
                    arguments = unabreviatedArguments,
                    isMarkedNullable = type.isMarkedNullable
                )
        }
        is CirTypeAliasType -> unabbreviate(computeExpandedType(type))
    }

    private fun createClassTypeWithAllOuterTypes(
        classDescriptor: ClassDescriptor,
        arguments: List<CirTypeProjection>,
        isMarkedNullable: Boolean
    ): CirClassType {
        val outerType: CirClassType?
        val remainingArguments: List<CirTypeProjection>

        if (classDescriptor.isInner) {
            val declaredTypeParametersCount = classDescriptor.declaredTypeParameters.size
            outerType = createClassTypeWithAllOuterTypes(
                classDescriptor = classDescriptor.containingDeclaration as ClassDescriptor,
                arguments = arguments.subList(declaredTypeParametersCount, arguments.size),
                isMarkedNullable = false // don't pass nullable flag to outer types
            )
            remainingArguments = arguments.subList(0, declaredTypeParametersCount)
        } else {
            outerType = null
            remainingArguments = arguments
        }

        return createClassType(
            classId = classDescriptor.classifierId,
            outerType = outerType,
            visibility = classDescriptor.visibility,
            arguments = remainingArguments,
            isMarkedNullable = isMarkedNullable
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun createArguments(arguments: List<TypeProjection>): List<CirTypeProjection> =
        arguments.compactMap { projection ->
            if (projection.isStarProjection)
                CirStarTypeProjection
            else
                CirTypeProjectionImpl(
                    projectionKind = projection.projectionKind,
                    type = create(projection.type)
                )
        }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun createArguments(arguments: List<KmTypeProjection>, typeResolver: CirTypeResolver): List<CirTypeProjection> {
        return arguments.compactMap { argument ->
            val variance = argument.variance ?: return@compactMap CirStarTypeProjection
            val argumentType = argument.type ?: return@compactMap CirStarTypeProjection

            CirTypeProjectionImpl(
                projectionKind = decodeVariance(variance),
                type = create(argumentType, typeResolver)
            )
        }
    }

    private inline val TypeParameterDescriptor.typeParameterIndex: Int
        get() {
            var index = index
            var parent = containingDeclaration

            if (parent is CallableMemberDescriptor) {
                parent = parent.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: return index
                index += parent.declaredTypeParameters.size
            }

            while (parent is ClassifierDescriptorWithTypeParameters) {
                parent = parent.containingDeclaration as? ClassifierDescriptorWithTypeParameters ?: break
                index += parent.declaredTypeParameters.size
            }

            return index
        }
}

typealias TypeParameterId = Int
typealias TypeParameterIndex = Int

abstract class CirTypeResolver : TypeParameterResolver {
    abstract val providedClassifiers: CirProvidedClassifiers
    protected abstract val typeParameterIndexOffset: Int

    inline fun <reified T : CirProvided.Classifier> resolveClassifier(classifierId: CirEntityId): T {
        val classifier = providedClassifiers.classifier(classifierId)
            ?: error("Unresolved classifier: $classifierId")

        check(classifier is T) {
            "Resolved classifier $classifierId of type ${classifier::class.java.simpleName}. Expected: ${T::class.java.simpleName}."
        }

        return classifier
    }

    abstract fun resolveTypeParameterIndex(id: TypeParameterId): TypeParameterIndex
    abstract override fun resolveTypeParameter(id: TypeParameterId): KmTypeParameter

    private class TopLevel(override val providedClassifiers: CirProvidedClassifiers) : CirTypeResolver() {
        override val typeParameterIndexOffset get() = 0

        override fun resolveTypeParameterIndex(id: TypeParameterId) = error("Unresolved type parameter: id=$id")
        override fun resolveTypeParameter(id: TypeParameterId) = error("Unresolved type parameter: id=$id")
    }

    private class Nested(
        private val parent: CirTypeResolver,
        private val typeParameterMapping: TIntObjectHashMap<TypeParameterInfo>
    ) : CirTypeResolver() {
        override val providedClassifiers get() = parent.providedClassifiers
        override val typeParameterIndexOffset = typeParameterMapping.size() + parent.typeParameterIndexOffset

        override fun resolveTypeParameterIndex(id: TypeParameterId) =
            typeParameterMapping[id]?.index ?: parent.resolveTypeParameterIndex(id)

        override fun resolveTypeParameter(id: TypeParameterId) =
            typeParameterMapping[id]?.typeParameter ?: parent.resolveTypeParameter(id)
    }

    private class TypeParameterInfo(val index: TypeParameterIndex, val typeParameter: KmTypeParameter)

    fun create(typeParameters: List<KmTypeParameter>): CirTypeResolver =
        if (typeParameters.isEmpty())
            this
        else {
            val mapping = TIntObjectHashMap<TypeParameterInfo>(typeParameters.size * 2)
            typeParameters.forEachIndexed { localIndex, typeParameter ->
                val typeParameterInfo = TypeParameterInfo(
                    index = localIndex + typeParameterIndexOffset,
                    typeParameter = typeParameter
                )
                mapping.put(typeParameter.id, typeParameterInfo)
            }

            Nested(this, mapping)
        }

    companion object {
        fun create(providedClassifiers: CirProvidedClassifiers): CirTypeResolver = TopLevel(providedClassifiers)
    }
}
