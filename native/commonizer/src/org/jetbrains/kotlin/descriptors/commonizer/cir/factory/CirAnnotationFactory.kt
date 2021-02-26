/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import gnu.trove.THashMap
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirClassType
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirName
import org.jetbrains.kotlin.descriptors.commonizer.cir.impl.CirAnnotationImpl
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.compact
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue

object CirAnnotationFactory {
    private val interner = Interner<CirAnnotation>()

    fun create(source: AnnotationDescriptor): CirAnnotation {
        val type = CirTypeFactory.create(source.type) as CirClassType

        val allValueArguments: Map<Name, ConstantValue<*>> = source.allValueArguments
        if (allValueArguments.isEmpty())
            return create(type = type, constantValueArguments = emptyMap(), annotationValueArguments = emptyMap())

        val constantValueArguments: MutableMap<CirName, CirConstantValue<*>> = THashMap(allValueArguments.size)
        val annotationValueArguments: MutableMap<CirName, CirAnnotation> = THashMap(allValueArguments.size)

        allValueArguments.forEach { (name, constantValue) ->
            val cirName = CirName.create(name)
            if (constantValue is AnnotationValue)
                annotationValueArguments[cirName] = create(source = constantValue.value)
            else
                constantValueArguments[cirName] = CirConstantValueFactory.createSafely(
                    constantValue = constantValue,
                    constantName = cirName,
                    owner = source,
                )
        }

        return create(
            type = type,
            constantValueArguments = constantValueArguments.compact(),
            annotationValueArguments = annotationValueArguments.compact()
        )
    }

    fun create(
        type: CirClassType,
        constantValueArguments: Map<CirName, CirConstantValue<*>>,
        annotationValueArguments: Map<CirName, CirAnnotation>
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
