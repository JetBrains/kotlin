/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kotlin.script.experimental.api

import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.expressions.FirLiteralExpression
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.classId
import org.jetbrains.kotlin.name.Name

fun FirAnnotation.toAnnotationObjectIfMatches(vararg expectedAnnClasses: Class<out Annotation>): Annotation? {
    val shortName = when(val typeRef = annotationTypeRef) {
        is FirResolvedTypeRef -> typeRef.coneType.classId?.shortClassName ?: return null
        is FirUserTypeRef -> typeRef.qualifier.last().name
        else -> return null
    }.asString()
    val expectedAnnClass = expectedAnnClasses.firstOrNull { it.simpleName == shortName } ?: return null
    val ctor = expectedAnnClass.constructors.firstOrNull() ?: return null
    val argsMap = LinkedHashMap<String, Any?>()
    ctor.parameters.forEach {
        argsMap[it.name] =
            when(val arg = argumentMapping.mapping[Name.identifier(it.name)]) {
                // TODO: classrefs?
                is FirLiteralExpression -> arg.value
                else -> null
            }
    }
    return ctor.newInstance(*argsMap.values.toTypedArray()) as Annotation
}

