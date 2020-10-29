/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirClassConstructorImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapNotNull

object CirClassConstructorFactory {
    fun create(source: ClassConstructorDescriptor): CirClassConstructor {
        check(source.kind == CallableMemberDescriptor.Kind.DECLARATION) {
            "Unexpected ${CallableMemberDescriptor.Kind::class.java} for class constructor $source, ${source::class.java}: ${source.kind}"
        }

        return create(
            annotations = source.annotations.compactMap(CirAnnotationFactory::create),
            typeParameters = source.typeParameters.compactMapNotNull { typeParameter ->
                // save only type parameters that are contributed by the constructor itself
                typeParameter.takeIf { it.containingDeclaration == source }?.let(CirTypeParameterFactory::create)
            },
            visibility = source.visibility,
            containingClassDetails = CirContainingClassDetailsFactory.create(source),
            valueParameters = source.valueParameters.compactMap(CirValueParameterFactory::create),
            hasStableParameterNames = source.hasStableParameterNames(),
            isPrimary = source.isPrimary
        )
    }

    @Suppress("NOTHING_TO_INLINE")
    inline fun create(
        annotations: List<CirAnnotation>,
        typeParameters: List<CirTypeParameter>,
        visibility: DescriptorVisibility,
        containingClassDetails: CirContainingClassDetails,
        valueParameters: List<CirValueParameter>,
        hasStableParameterNames: Boolean,
        isPrimary: Boolean
    ): CirClassConstructor {
        return CirClassConstructorImpl(
            annotations = annotations,
            typeParameters = typeParameters,
            visibility = visibility,
            containingClassDetails = containingClassDetails,
            valueParameters = valueParameters,
            hasStableParameterNames = hasStableParameterNames,
            isPrimary = isPrimary
        )
    }
}
