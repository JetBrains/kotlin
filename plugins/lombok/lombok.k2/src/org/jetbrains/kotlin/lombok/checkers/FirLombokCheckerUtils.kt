/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.types.lookupTagIfAny
import org.jetbrains.kotlin.fir.types.resolvedType
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.config.CallSuperMode
import org.jetbrains.kotlin.lombok.config.ConeLombokAnnotations
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds

/**
 * Mirrors Lombok behavior: when `*.callSuper=warn` is configured and the
 * annotated class has a non-trivial superclass, warn that the generated function (`toString` or `equals`/`hashCode`) will
 * not chain to it.
 */
context(context: CheckerContext, reporter: DiagnosticReporter)
fun checkCallSuper(
    callSuperMode: CallSuperMode,
    annotationInfo: ConeLombokAnnotations.ConeLombokAnnotation,
    declaration: FirRegularClass,
    functionNames: Set<Name>,
) {
    if (callSuperMode == CallSuperMode.Warn &&
        declaration.symbol.getSuperClassSymbolOrAny(context.session).let { it != null && it.classId != StandardClassIds.Any }
    ) {
        reporter.reportOn(
            annotationInfo.annotation.source,
            LombokFirDiagnostics.CALL_SUPER_NOT_CALLED,
            functionNames.joinToString("/"),
            annotationInfo.annotation.resolvedType.lookupTagIfAny?.name!!,
            context,
        )
    }
}
