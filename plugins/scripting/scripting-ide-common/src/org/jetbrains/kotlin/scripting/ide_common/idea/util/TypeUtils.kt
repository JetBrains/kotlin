/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("TypeUtils")

package org.jetbrains.kotlin.scripting.ide_common.idea.util

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.jetbrains.kotlin.types.typeUtil.substitute

// Copy-pasted from Kotlin plugin in intellij-community
fun KotlinType.approximateFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false
): KotlinType {
    if (isDynamic()) return this
    return unwrapEnhancement().approximateNonDynamicFlexibleTypes(preferNotNull, preferStarForRaw)
}

// Copy-pasted from Kotlin plugin in intellij-community
private fun KotlinType.approximateNonDynamicFlexibleTypes(
    preferNotNull: Boolean = false,
    preferStarForRaw: Boolean = false
): SimpleType {
    if (this is ErrorType) return this

    if (isFlexible()) {
        val flexible = asFlexibleType()
        val lowerBound = flexible.lowerBound
        val upperBound = flexible.upperBound
        val lowerClass = lowerBound.constructor.declarationDescriptor as? ClassDescriptor?
        val isCollection = lowerClass != null && JavaToKotlinClassMapper.isMutable(lowerClass)
        // (Mutable)Collection<T>! -> MutableCollection<T>?
        // Foo<(Mutable)Collection<T>!>! -> Foo<Collection<T>>?
        // Foo! -> Foo?
        // Foo<Bar!>! -> Foo<Bar>?
        var approximation =
            if (isCollection)
            // (Mutable)Collection<T>!
                if (lowerBound.isMarkedNullable != upperBound.isMarkedNullable)
                    lowerBound.makeNullableAsSpecified(!preferNotNull)
                else
                    lowerBound
            else
                if (this is RawType && preferStarForRaw) upperBound.makeNullableAsSpecified(!preferNotNull)
                else
                    if (preferNotNull) lowerBound else upperBound

        approximation = approximation.approximateNonDynamicFlexibleTypes()

        approximation = if (nullability() == TypeNullability.NOT_NULL) approximation.makeNullableAsSpecified(false) else approximation

        if (approximation.isMarkedNullable && !lowerBound
                .isMarkedNullable && TypeUtils.isTypeParameter(approximation) && TypeUtils.hasNullableSuperType(approximation)
        ) {
            approximation = approximation.makeNullableAsSpecified(false)
        }

        return approximation
    }

    (unwrap() as? AbbreviatedType)?.let {
        return AbbreviatedType(it.expandedType, it.abbreviation.approximateNonDynamicFlexibleTypes(preferNotNull))
    }
    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        annotations,
        constructor,
        arguments.map { it.substitute { type -> type.approximateFlexibleTypes(preferNotNull = true) } },
        isMarkedNullable,
        ErrorUtils.createErrorScope("This type is not supposed to be used in member resolution", true)
    )
}
