/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_common.util

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.load.java.descriptors.JavaCallableMemberDescriptor
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.types.AbstractTypeChecker
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeCheckerState
import org.jetbrains.kotlin.types.TypeConstructor
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.types.model.TypeConstructorMarker

fun descriptorsEqualWithSubstitution(
    descriptor1: DeclarationDescriptor?,
    descriptor2: DeclarationDescriptor?,
    checkOriginals: Boolean = true
): Boolean {
    if (descriptor1 == descriptor2) return true
    if (descriptor1 == null || descriptor2 == null) return false
    if (checkOriginals && descriptor1.original != descriptor2.original) return false
    if (descriptor1 !is CallableDescriptor) return true
    descriptor2 as CallableDescriptor

    val typeCheckerState = object : TypeCheckerState(
        isErrorTypeEqualsToAnything = false,
        isStubTypeEqualsToAnything = true,
        allowedTypeVariable = true,
        SimpleClassicTypeSystemContext,
        KotlinTypePreparator.Default,
        KotlinTypeRefiner.Default
    ) {
        override fun customAreEqualTypeConstructors(c1: TypeConstructorMarker, c2: TypeConstructorMarker): Boolean {
            c1 as TypeConstructor
            c2 as TypeConstructor
            val typeParam1 = c1.declarationDescriptor as? TypeParameterDescriptor
            val typeParam2 = c2.declarationDescriptor as? TypeParameterDescriptor
            if (typeParam1 != null
                && typeParam2 != null
                && typeParam1.containingDeclaration == descriptor1
                && typeParam2.containingDeclaration == descriptor2
            ) {
                return typeParam1.index == typeParam2.index
            }

            return c1 == c2
        }
    }

    if (!AbstractTypeChecker.equalTypesOrNulls(typeCheckerState, descriptor1.returnType, descriptor2.returnType)) return false

    val parameters1 = descriptor1.valueParameters
    val parameters2 = descriptor2.valueParameters
    if (parameters1.size != parameters2.size) return false
    for ((param1, param2) in parameters1.zip(parameters2)) {
        if (!AbstractTypeChecker.equalTypes(typeCheckerState, param1.type, param2.type)) return false
    }
    return true
}

private fun AbstractTypeChecker.equalTypesOrNulls(state: TypeCheckerState, type1: KotlinType?, type2: KotlinType?): Boolean {
    if (type1 === type2) return true
    if (type1 == null || type2 == null) return false
    return equalTypes(state, type1, type2)
}

fun TypeConstructor.supertypesWithAny(): Collection<KotlinType> {
    val supertypes = supertypes
    val noSuperClass = supertypes.map { it.constructor.declarationDescriptor as? ClassDescriptor }.all {
        it == null || it.kind == ClassKind.INTERFACE
    }
    return if (noSuperClass) supertypes + builtIns.anyType else supertypes
}

val DeclarationDescriptor.isJavaDescriptor
    get() = this is JavaClassDescriptor || this is JavaCallableMemberDescriptor