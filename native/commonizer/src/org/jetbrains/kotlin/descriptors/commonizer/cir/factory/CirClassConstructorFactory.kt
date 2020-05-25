/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassConstructorImpl

object CirClassConstructorFactory {
    fun create(source: ClassConstructorDescriptor): CirClassConstructor {
        val containingClass: ClassDescriptor = source.containingDeclaration

        return create(
            annotations = source.annotations.map(CirAnnotationFactory::create),
            typeParameters = source.typeParameters.mapNotNull { typeParameter ->
                // save only type parameters that are contributed by the constructor itself
                typeParameter.takeIf { it.containingDeclaration == source }?.let(CirTypeParameterFactory::create)
            },
            visibility = source.visibility,
            containingClassDetails = CirContainingClassDetails(
                kind = containingClass.kind,
                modality = containingClass.modality,
                isData = containingClass.isData
            ),
            valueParameters = source.valueParameters.map(CirValueParameterFactory::create),
            hasStableParameterNames = source.hasStableParameterNames(),
            hasSynthesizedParameterNames = source.hasSynthesizedParameterNames(),
            isPrimary = source.isPrimary,
            kind = source.kind
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        typeParameters: List<CirTypeParameter>,
        visibility: Visibility,
        containingClassDetails: CirContainingClassDetails,
        valueParameters: List<CirValueParameter>,
        hasStableParameterNames: Boolean,
        hasSynthesizedParameterNames: Boolean,
        isPrimary: Boolean,
        kind: CallableMemberDescriptor.Kind
    ): CirClassConstructor {
        return CirClassConstructorImpl(
            annotations = annotations,
            typeParameters = typeParameters,
            visibility = visibility,
            containingClassDetails = containingClassDetails,
            valueParameters = valueParameters,
            hasStableParameterNames = hasStableParameterNames,
            hasSynthesizedParameterNames = hasSynthesizedParameterNames,
            isPrimary = isPrimary,
            kind = kind
        )
    }
}
