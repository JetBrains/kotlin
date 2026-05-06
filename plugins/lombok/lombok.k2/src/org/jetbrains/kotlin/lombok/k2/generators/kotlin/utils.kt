/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.generators.kotlin

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.getAnnotationByClassId
import org.jetbrains.kotlin.fir.declarations.utils.isExtension
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.hasContextParameters
import org.jetbrains.kotlin.name.ClassId

val FirCallableSymbol<*>.isRelevantForConflictsCheck: Boolean
    get() = !isExtension && !hasContextParameters

/**
 * Annotations on primary constructor val/var params with @Target(FIELD) end up in the
 * backing field's annotation list, not in property.annotations. We must check both.
 */
fun FirPropertySymbol.findAnnotationOnPropertyOrField(classId: ClassId, session: FirSession): FirAnnotation? =
    getAnnotationByClassId(classId, session) ?: backingFieldSymbol?.getAnnotationByClassId(classId, session)
