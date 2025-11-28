/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.utils.tryCreateCallableMapping
import kotlin.reflect.KClass

fun FirAnnotationCall.toAnnotationObjectIfMatches(vararg expectedAnnClasses: KClass<out Annotation>): ResultWithDiagnostics<Annotation>? {
    val shortName = when(val typeRef = annotationTypeRef) {
        is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName ?: return null
        is FirUserTypeRef -> typeRef.qualifier.last().name
        else -> return null
    }.asString()
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null
    val mapping =
        tryCreateCallableMapping(
            ctor,
            argumentList.arguments.map {
                when (it) {
                    // TODO: classrefs?
                    is FirLiteralExpression -> it.value
                    else -> null
                }
            }
        )
    if (mapping != null) {
        try {
            return ctor.callBy(mapping).asSuccess()
        } catch (e: Exception) { // TODO: find the exact exception type thrown then callBy fails
            return makeFailureResult(e.asDiagnostics())
        }
    }
    return null
}

