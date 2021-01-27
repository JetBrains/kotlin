/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptorWithTypeParameters
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirTypeSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.types.*
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

internal val ClassifierDescriptorWithTypeParameters.internedClassId: ClassId
    get() = when (val owner = containingDeclaration) {
        is PackageFragmentDescriptor -> internedClassId(owner.fqName.intern(), name.intern())
        is ClassDescriptor -> internedClassId(owner.internedClassId, name.intern())
        else -> error("Unexpected containing declaration type for $this: ${owner::class}, $owner")
    }

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
            append(":[")
            typeParameterDescriptor.upperBounds.forEachIndexed { index, upperBound ->
                if (index > 0)
                    append(",")
                buildTypeSignature(upperBound, exploredTypeParameters)
            }
            append("]")
        }

        if (type.isMarkedNullable)
            append("?")
    } else {
        // N.B. this is classifier type
        val abbreviation = (type as? AbbreviatedType)?.abbreviation ?: type
        append(abbreviation.declarationDescriptor.classId!!.asString())

        val arguments = type.arguments
        if (arguments.isNotEmpty()) {
            append("<")
            arguments.forEachIndexed { index, argument ->
                if (index > 0)
                    append(",")

                if (argument.isStarProjection)
                    append("*")
                else {
                    val variance = argument.projectionKind
                    if (variance != Variance.INVARIANT)
                        append(variance).append(" ")
                    buildTypeSignature(argument.type, exploredTypeParameters)
                }
            }
            append(">")
        }

        if (abbreviation.isMarkedNullable)
            append("?")
    }
}

// dedicated to hold unique entries of "signature"
private val typeSignatureInterner = Interner<CirTypeSignature>()
