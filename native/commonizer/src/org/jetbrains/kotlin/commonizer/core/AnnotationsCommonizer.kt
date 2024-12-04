/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.commonizer.cir.CirClassType
import org.jetbrains.kotlin.commonizer.cir.CirConstantValue
import org.jetbrains.kotlin.commonizer.cir.CirEntityId
import org.jetbrains.kotlin.commonizer.utils.COMMONIZER_OBJC_INTEROP_CALLABLE_ANNOTATION_ID
import org.jetbrains.kotlin.commonizer.utils.DEPRECATED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.commonizer.utils.isObjCInteropCallableAnnotation

private typealias Annotations = Map<CirEntityId, CirAnnotation>

object AnnotationsCommonizer : AssociativeCommonizer<List<CirAnnotation>> {

    override fun commonize(first: List<CirAnnotation>, second: List<CirAnnotation>): List<CirAnnotation> {
        val firstAnnotations = first.associateBy { it.type.classifierId }
        val secondAnnotations = second.associateBy { it.type.classifierId }

        return buildList {
            addAll(commonizedObjcCallableAnnotation(first, second))
            commonizedDeprecatedAnnotation(firstAnnotations, secondAnnotations)?.let { add(it) }
            addAll(commonizedSimpleAnnotations(firstAnnotations, secondAnnotations))
        }.distinct()
    }

    private fun commonizedObjcCallableAnnotation(firstAnnotations: List<CirAnnotation>, secondAnnotations: List<CirAnnotation>): List<CirAnnotation> {
        val firstObjCCallable = firstAnnotations.filter { it.type.classifierId.isObjCInteropCallableAnnotation }
        val secondObjCCallable = secondAnnotations.filter { it.type.classifierId.isObjCInteropCallableAnnotation }
        return buildList {
            if (firstObjCCallable.isNotEmpty() && secondObjCCallable.isNotEmpty()) {
                add(objCCallableAnnotation)
                firstObjCCallable.forEach { first ->
                    secondObjCCallable.forEach loop@{ second ->
                        if (first.type.classifierId == second.type.classifierId) {
                            val annotation = CirAnnotation.createInterned(
                                CirClassType.createInterned(
                                    classId = first.type.classifierId,
                                    outerType = null, arguments = emptyList(), isMarkedNullable = false
                                ),
                                buildMap {
                                    for ((key, value) in first.constantValueArguments) {
                                        if (second.constantValueArguments[key] == value) {
                                            put(key, value)
                                            continue
                                        }
                                        when (key.name) {
                                            "selector", "initSelector" -> return@loop
                                            "encoding" -> put(key, CirConstantValue.StringValue(""))
                                            "designated" -> put(key, CirConstantValue.BooleanValue(false))
                                            "isStret" -> {}
                                            else -> error("No default is known for $key")
                                        }
                                    }
                                },
                                emptyMap()
                            )
                            add(annotation)
                        }
                    }
                }
            }
        }
    }

    private fun commonizedSimpleAnnotations(first: Annotations, second: Annotations): List<CirAnnotation> {
        fun CirAnnotation.isSimple() = type.arguments.isEmpty() && !type.classifierId.isObjCInteropCallableAnnotation &&
                annotationValueArguments.isEmpty() && constantValueArguments.isEmpty()

        return first.values.filter { annotation ->
            annotation.isSimple() && second[annotation.type.classifierId].let { secondAnnotation ->
                secondAnnotation != null && secondAnnotation.isSimple()
            }
        }
    }

    private fun commonizedDeprecatedAnnotation(first: Annotations, second: Annotations): CirAnnotation? {
        val firstDeprecation = first[DEPRECATED_ANNOTATION_CLASS_ID] ?: return null
        val secondDeprecation = second[DEPRECATED_ANNOTATION_CLASS_ID] ?: return null
        return DeprecationAnnotationCommonizer.commonize(firstDeprecation, secondDeprecation)
    }

    private val objCCallableAnnotation = CirAnnotation.createInterned(
        CirClassType.createInterned(
            classId = COMMONIZER_OBJC_INTEROP_CALLABLE_ANNOTATION_ID,
            outerType = null, arguments = emptyList(), isMarkedNullable = false
        ),
        constantValueArguments = emptyMap(),
        annotationValueArguments = emptyMap()
    )
}
