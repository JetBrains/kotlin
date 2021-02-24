/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirPackageName
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
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

internal inline val ClassDescriptor.filteredSupertypes: Collection<KotlinType>
    get() = typeConstructor.supertypes.takeUnless { it.size == 1 && KotlinBuiltIns.isAny(it.first()) } ?: emptyList()

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

// dedicated to hold unique entries of "signature"
private val typeSignatureInterner = Interner<CirTypeSignature>()
