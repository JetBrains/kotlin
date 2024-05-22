/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.rhizomedb.fir.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.analysis.checkers.toRegularClassSymbol
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.utils.isAbstract
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isSealed
import org.jetbrains.rhizomedb.fir.checkers.RhizomedbFirErrors.WRONG_ENTITY_TYPE_TARGET
import org.jetbrains.rhizomedb.fir.extensions.rhizomedbPredicateMatcher
import org.jetbrains.rhizomedb.fir.getEntityTypeAnnotation

object RhizomedbFirEntityTypeChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirRegularClass, context: CheckerContext, reporter: DiagnosticReporter) {
        val session = context.session
        val generateEntityTypeAnnotation = declaration.getEntityTypeAnnotation(session) ?: return
        val predicateMatcher = session.rhizomedbPredicateMatcher
        val wrongTargetReason = when {
            declaration.classKind != ClassKind.CLASS -> WrongEntityTypeTarget.NOT_CLASS
            declaration.isAbstract || declaration.isSealed -> WrongEntityTypeTarget.ABSTRACT
            !predicateMatcher.isEntity(declaration.symbol) -> WrongEntityTypeTarget.NOT_ENTITY
            !predicateMatcher.hasFromEidConstructor(declaration.symbol) -> WrongEntityTypeTarget.NO_CONSTRUCTOR
            else -> {
                val companion = declaration.companionObjectSymbol ?: return
                val superclasses = companion.resolvedSuperTypeRefs.filter { it.source != null }
                if (superclasses.any { it.toRegularClassSymbol(session)?.isInterface == false }) {
                    WrongEntityTypeTarget.ALREADY_EXTENDS
                } else {
                    null
                }
            }
        }
        wrongTargetReason?.let {
            reporter.reportOn(generateEntityTypeAnnotation.source, WRONG_ENTITY_TYPE_TARGET, it, context)
        }
    }
}