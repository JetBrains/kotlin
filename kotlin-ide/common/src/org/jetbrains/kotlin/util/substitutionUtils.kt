/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.types.substitutions

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.TypeCheckingProcedure
import java.util.*

fun getTypeSubstitution(baseType: KotlinType, derivedType: KotlinType): LinkedHashMap<TypeConstructor, TypeProjection>? {
    val substitutedType = TypeCheckingProcedure.findCorrespondingSupertype(derivedType, baseType) ?: return null

    val substitution = LinkedHashMap<TypeConstructor, TypeProjection>(substitutedType.arguments.size)
    for ((param, arg) in baseType.constructor.parameters.zip(substitutedType.arguments)) {
        substitution[param.typeConstructor] = arg
    }

    return substitution
}

fun getCallableSubstitution(
    baseCallable: CallableDescriptor,
    derivedCallable: CallableDescriptor
): MutableMap<TypeConstructor, TypeProjection>? {
    val baseClass = baseCallable.containingDeclaration as? ClassDescriptor ?: return null
    val derivedClass = derivedCallable.containingDeclaration as? ClassDescriptor ?: return null
    val substitution = getTypeSubstitution(baseClass.defaultType, derivedClass.defaultType) ?: return null

    for ((baseParam, derivedParam) in baseCallable.typeParameters.zip(derivedCallable.typeParameters)) {
        substitution[baseParam.typeConstructor] = TypeProjectionImpl(derivedParam.defaultType)
    }

    return substitution
}

fun getCallableSubstitutor(
    baseCallable: CallableDescriptor,
    derivedCallable: CallableDescriptor
): TypeSubstitutor? {
    return getCallableSubstitution(baseCallable, derivedCallable)?.let { TypeSubstitutor.create(it) }
}

fun getTypeSubstitutor(baseType: KotlinType, derivedType: KotlinType): TypeSubstitutor? {
    return getTypeSubstitution(baseType, derivedType)?.let { TypeSubstitutor.create(it) }
}
