/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirSimpleType
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirAnnotationImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.Interner
import org.jetbrains.kotlin.descriptors.commonizer.utils.checkConstantSupportedInCommonization
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue

object CirAnnotationFactory {
    private val interner = Interner<CirAnnotation>()

    fun create(source: AnnotationDescriptor): CirAnnotation {
        val type = CirTypeFactory.create(source.type) as CirSimpleType

        val allValueArguments: Map<Name, ConstantValue<*>> = source.allValueArguments
        if (allValueArguments.isEmpty())
            return create(type = type, constantValueArguments = emptyMap(), annotationValueArguments = emptyMap())

        val constantValueArguments: MutableMap<Name, ConstantValue<*>> = THashMap()
        val annotationValueArguments: MutableMap<Name, CirAnnotation> = THashMap()

        allValueArguments.forEach { (name, constantValue) ->
            checkConstantSupportedInCommonization(
                constantValue = constantValue,
                constantName = name,
                owner = source,
                allowAnnotationValues = true
            )

            if (constantValue is AnnotationValue)
                annotationValueArguments[name.intern()] = create(source = constantValue.value)
            else
                constantValueArguments[name.intern()] = constantValue
        }

        return create(
            type = type,
            constantValueArguments = constantValueArguments,
            annotationValueArguments = annotationValueArguments
        )
    }

    fun create(
        type: CirSimpleType,
        constantValueArguments: Map<Name, ConstantValue<*>>,
        annotationValueArguments: Map<Name, CirAnnotation>
    ): CirAnnotation {
        return interner.intern(
            CirAnnotationImpl(
                type = type,
                constantValueArguments = constantValueArguments,
                annotationValueArguments = annotationValueArguments
            )
        )
    }
}
