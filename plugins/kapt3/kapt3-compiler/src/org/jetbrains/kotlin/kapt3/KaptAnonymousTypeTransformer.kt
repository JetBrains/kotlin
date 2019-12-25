/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.kapt3

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.resolve.DeclarationSignatureAnonymousTypeTransformer
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils.*
import org.jetbrains.kotlin.resolve.jvm.extensions.PartialAnalysisHandlerExtension
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.builtIns

class KaptAnonymousTypeTransformer(
    private val analysisExtension: PartialAnalysisHandlerExtension
) : DeclarationSignatureAnonymousTypeTransformer {
    override fun transformAnonymousType(descriptor: DeclarationDescriptorWithVisibility, type: KotlinType): KotlinType? {
        if (!analysisExtension.analyzePartially) {
            return null
        }

        if (isLocal(descriptor)) {
            return type
        }

        return convertPossiblyAnonymousType(type)
    }
}

internal fun convertPossiblyAnonymousType(type: KotlinType): KotlinType {
    val declaration = type.constructor.declarationDescriptor as? ClassDescriptor ?: return type

    if (KotlinBuiltIns.isArray(type)) {
        val elementTypeProjection = type.arguments.singleOrNull()
        if (elementTypeProjection != null && !elementTypeProjection.isStarProjection) {
            return type.builtIns.getArrayType(
                elementTypeProjection.projectionKind,
                convertPossiblyAnonymousType(elementTypeProjection.type)
            )
        }
    }

    val actualType = when {
        isAnonymousObject(declaration) || DescriptorUtils.isLocal(declaration) -> {
            if (type.constructor.supertypes.size == 1) {
                convertPossiblyAnonymousType(type.constructor.supertypes.iterator().next())
            } else {
                /*
                    Frontend reports an error on public properties in this case,
                    but we ignore errors when making stubs, so there should be a reasonable fallback.
                 */
                type.builtIns.anyType
            }
        }
        else -> type
    }

    if (actualType.arguments.isEmpty()) return actualType

    val arguments = actualType.arguments.map { typeArg ->
        if (typeArg.isStarProjection)
            return@map typeArg

        TypeProjectionImpl(typeArg.projectionKind, convertPossiblyAnonymousType(typeArg.type))
    }

    return actualType.replace(newArguments = arguments)
}