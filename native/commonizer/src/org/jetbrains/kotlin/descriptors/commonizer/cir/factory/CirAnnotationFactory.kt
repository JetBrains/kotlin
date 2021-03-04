/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.cir.factory

import gnu.trove.THashMap
import kotlinx.metadata.Flag
import kotlinx.metadata.Flags
import kotlinx.metadata.KmAnnotation
import kotlinx.metadata.KmAnnotationArgument
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.CirProvided
import org.jetbrains.kotlin.descriptors.commonizer.utils.compact
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap

object CirAnnotationFactory {
    fun createAnnotations(flags: Flags, typeResolver: CirTypeResolver, annotations: () -> List<KmAnnotation>): List<CirAnnotation> {
        return if (!Flag.Common.HAS_ANNOTATIONS(flags))
            emptyList()
        else
            annotations().compactMap { create(it, typeResolver) }
    }

    fun create(source: KmAnnotation, typeResolver: CirTypeResolver): CirAnnotation {
        val classId = CirEntityId.create(source.className)
        val clazz: CirProvided.Class = typeResolver.resolveClassifier(classId)

        val type = CirTypeFactory.createClassType(
            classId = classId,
            outerType = null, // annotation class can't be inner class
            visibility = clazz.visibility,
            arguments = clazz.typeParameters.compactMap { typeParameter ->
                CirTypeProjectionImpl(
                    projectionKind = typeParameter.variance,
                    type = CirTypeFactory.createTypeParameterType(
                        index = typeParameter.index,
                        isMarkedNullable = false
                    )
                )
            },
            isMarkedNullable = false
        )

        val allValueArguments: Map<String, KmAnnotationArgument<*>> = source.arguments
        if (allValueArguments.isEmpty())
            return CirAnnotation.createInterned(type = type, constantValueArguments = emptyMap(), annotationValueArguments = emptyMap())

        val constantValueArguments: MutableMap<CirName, CirConstantValue<*>> = THashMap(allValueArguments.size)
        val annotationValueArguments: MutableMap<CirName, CirAnnotation> = THashMap(allValueArguments.size)

        allValueArguments.forEach { (name, constantValue) ->
            val cirName = CirName.create(name)
            if (constantValue is KmAnnotationArgument.AnnotationValue)
                annotationValueArguments[cirName] = create(source = constantValue.value, typeResolver)
            else
                constantValueArguments[cirName] = CirConstantValueFactory.createSafely(
                    constantValue = constantValue,
                    constantName = cirName,
                    owner = source,
                )
        }

        return CirAnnotation.createInterned(
            type = type,
            constantValueArguments = constantValueArguments.compact(),
            annotationValueArguments = annotationValueArguments.compact()
        )
    }
}
