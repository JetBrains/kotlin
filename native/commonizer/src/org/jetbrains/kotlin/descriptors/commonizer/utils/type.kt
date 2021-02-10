/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import gnu.trove.TIntHashSet
import kotlinx.metadata.*
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.TypeParameterResolver
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.isNullableAny
import org.jetbrains.kotlin.types.typeUtil.makeNotNullable

internal inline val KotlinType.declarationDescriptor: ClassifierDescriptor
    get() = (constructor.declarationDescriptor ?: error("No declaration descriptor found for $constructor"))

// eliminate unnecessary repeated abbreviations
internal fun extractExpandedType(abbreviated: AbbreviatedType): SimpleType {
    var expanded = abbreviated.expandedType
    while (expanded is AbbreviatedType) {
        if (expanded.abbreviation.declarationDescriptor !== abbreviated.abbreviation.declarationDescriptor)
            break
        else
            expanded = expanded.expandedType
    }
    return expanded
}

internal val ClassifierDescriptorWithTypeParameters.classifierId: CirEntityId
    get() = when (val owner = containingDeclaration) {
        is PackageFragmentDescriptor -> CirEntityId.create(
            packageName = CirPackageName.create(owner.fqName),
            relativeName = CirName.create(name)
        )
        is ClassDescriptor -> owner.classifierId.createNestedEntityId(CirName.create(name))
        else -> error("Unexpected containing declaration type for $this: ${owner::class}, $owner")
    }

internal inline val TypeParameterDescriptor.filteredUpperBounds: List<KotlinType>
    get() = upperBounds.takeUnless { it.singleOrNull()?.isNullableAny() == true } ?: emptyList()

internal inline val KmTypeParameter.filteredUpperBounds: List<KmType>
    get() = upperBounds.takeUnless { it.singleOrNull()?.isNullableAny == true } ?: emptyList()

internal inline val ClassDescriptor.filteredSupertypes: Collection<KotlinType>
    get() = typeConstructor.supertypes.takeUnless { it.size == 1 && KotlinBuiltIns.isAny(it.first()) } ?: emptyList()

internal inline val KmClass.filteredSupertypes: List<KmType>
    get() = supertypes.takeUnless { it.singleOrNull()?.isAny == true } ?: emptyList()

private inline val KmType.isNullableAny: Boolean
    get() = (classifier as? KmClassifier.Class)?.name == ANY_CLASS_FULL_NAME && Flag.Type.IS_NULLABLE(flags)

private inline val KmType.isAny: Boolean
    get() = (classifier as? KmClassifier.Class)?.name == ANY_CLASS_FULL_NAME && !Flag.Type.IS_NULLABLE(flags)

internal val KotlinType.signature: CirTypeSignature
    get() {
        // use of interner saves up to 95% of duplicates
        return typeSignatureInterner.intern(buildString { buildTypeSignature(this@signature, HashSet()) })
    }

private fun StringBuilder.buildTypeSignature(type: KotlinType, exploredTypeParameters: MutableSet<KotlinType>) {
    val typeParameterDescriptor = TypeUtils.getTypeParameterDescriptorOrNull(type)
    if (typeParameterDescriptor != null) {
        // N.B this is type parameter type
        append(typeParameterDescriptor.name.asString())

        if (exploredTypeParameters.add(type.makeNotNullable())) { // print upper bounds once the first time when type parameter type is met
            append(':').append('[')
            typeParameterDescriptor.filteredUpperBounds.forEachIndexed { index, upperBound ->
                if (index > 0)
                    append(',')
                buildTypeSignature(upperBound, exploredTypeParameters)
            }
            append(']')
        }

        if (type.isMarkedNullable)
            append('?')
    } else {
        // N.B. this is classifier type
        val abbreviation = (type as? AbbreviatedType)?.abbreviation ?: type
        append(abbreviation.declarationDescriptor.classId!!.asString())

        val arguments = abbreviation.arguments
        if (arguments.isNotEmpty()) {
            append('<')
            arguments.forEachIndexed { index, argument ->
                if (index > 0)
                    append(',')

                if (argument.isStarProjection)
                    append('*')
                else {
                    val variance = argument.projectionKind
                    if (variance != Variance.INVARIANT)
                        append(variance.label).append(' ')
                    buildTypeSignature(argument.type, exploredTypeParameters)
                }
            }
            append('>')
        }

        if (abbreviation.isMarkedNullable)
            append('?')
    }
}

internal fun KmType.computeSignature(typeParameterResolver: TypeParameterResolver): CirTypeSignature {
    // use of interner saves up to 95% of duplicates
    return typeSignatureInterner.intern(
        buildString {
            buildTypeSignature(
                type = this@computeSignature,
                typeParameterResolver = typeParameterResolver,
                exploredTypeParameters = TIntHashSet()
            )
        }
    )
}

private fun StringBuilder.buildTypeSignature(
    type: KmType,
    typeParameterResolver: TypeParameterResolver,
    exploredTypeParameters: TIntHashSet
) {
    when (val classifier = type.classifier) {
        is KmClassifier.TypeParameter -> {
            // N.B this is type parameter type
            val typeParameter = typeParameterResolver.resolveTypeParameter(classifier.id)
                ?: error("Unresolved type parameter #${classifier.id} in type ${type.classifier}")

            append(typeParameter.name)

            if (exploredTypeParameters.add(classifier.id)) { // print upper bounds once the first time when type parameter type is met
                append(':').append('[')
                typeParameter.filteredUpperBounds.forEachIndexed { index, upperBoundType ->
                    if (index > 0)
                        append(',')
                    buildTypeSignature(upperBoundType, typeParameterResolver, exploredTypeParameters)
                }
                append(']')
            }

            if (Flag.Type.IS_NULLABLE(type.flags))
                append('?')
        }
        else -> {
            val abbreviation = type.abbreviatedType ?: type

            val classifierId = when (val abbreviationClassifier = abbreviation.classifier) {
                is KmClassifier.Class -> abbreviationClassifier.name
                is KmClassifier.TypeAlias -> abbreviationClassifier.name
                else -> error("Unexpected classifier type for non-type-parameter type: ${type.classifier}")
            }
            append(classifierId)

            val arguments = abbreviation.arguments
            if (arguments.isNotEmpty()) {
                append('<')
                arguments.forEachIndexed { index, argument ->
                    if (index > 0)
                        append(',')

                    val variance = argument.variance
                    val argumentType = argument.type

                    if (variance == null || argumentType == null)
                        append('*')
                    else {
                        when (variance) {
                            KmVariance.INVARIANT -> Unit
                            KmVariance.IN -> append("in ")
                            KmVariance.OUT -> append("out ")
                        }
                        buildTypeSignature(argumentType, typeParameterResolver, exploredTypeParameters)
                    }
                }
                append('>')
            }

            if (Flag.Type.IS_NULLABLE(abbreviation.flags))
                append('?')
        }
    }
}

// dedicated to hold unique entries of "signature"
private val typeSignatureInterner = Interner<CirTypeSignature>()
