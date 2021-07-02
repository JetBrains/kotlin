/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.scripting.ide_common.idea.util

import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.checker.NewCapturedTypeConstructor
import org.jetbrains.kotlin.types.isDynamic
import org.jetbrains.kotlin.types.typeUtil.builtIns

// Copy-pasted from IdeDescriptorRenderers in intellij-community
object IdeDescriptorRenderersScripting {
    @JvmField
    val APPROXIMATE_FLEXIBLE_TYPES: (KotlinType) -> KotlinType = { it.approximateFlexibleTypes(preferNotNull = false) }

    private fun unwrapAnonymousType(type: KotlinType): KotlinType {
        if (type.isDynamic()) return type
        if (type.constructor is NewCapturedTypeConstructor) return type

        val classifier = type.constructor.declarationDescriptor
        if (classifier != null && !classifier.name.isSpecial) return type

        type.constructor.supertypes.singleOrNull()?.let { return it }

        val builtIns = type.builtIns
        return if (type.isMarkedNullable)
            builtIns.nullableAnyType
        else
            builtIns.anyType
    }

    private val BASE: DescriptorRenderer = DescriptorRenderer.withOptions {
        normalizedVisibilities = true
        withDefinedIn = false
        renderDefaultVisibility = false
        overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OVERRIDE
        unitReturnType = false
        enhancedTypes = true
        modifiers = DescriptorRendererModifier.ALL
        renderUnabbreviatedType = false
        annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
    }

    @JvmField
    val SOURCE_CODE: DescriptorRenderer = BASE.withOptions {
        classifierNamePolicy = ClassifierNamePolicy.SOURCE_CODE_QUALIFIED
        typeNormalizer = { APPROXIMATE_FLEXIBLE_TYPES(unwrapAnonymousType(it)) }
    }
}
