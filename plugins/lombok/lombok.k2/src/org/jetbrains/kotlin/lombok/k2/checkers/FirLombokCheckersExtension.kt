/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.k2.checkers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.DeclarationCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirBasicDeclarationChecker
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension

class FirLombokCheckersExtension(session: FirSession) : FirAdditionalCheckersExtension(session) {
    override val declarationCheckers: DeclarationCheckers = object : DeclarationCheckers() {
        override val basicDeclarationCheckers: Set<FirBasicDeclarationChecker> = setOf(
            FirLombokWrongOrUnsupportedAnnotationTargetChecker
        )

        override val regularClassCheckers: Set<FirRegularClassChecker> = setOf(
            FirLombokUsageChecker,
            FirLombokConflictingLogFieldChecker,
            FirLombokConflictingToStringChecker,
        )
    }
}
