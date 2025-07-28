/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization.compiler.fir.checkers

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirTypeAliasChecker
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

/**
 * Checker that reports a warning when kotlinx.serialization.Serializable is used as a typealias.
 * This is because K2 does not expand annotation typealiases on the COMPILER_REQUIRED_ANNOTATIONS phase.
 */
object FirSerializationTypeAliasChecker : FirTypeAliasChecker(MppCheckerKind.Common) {
    private val SERIALIZABLE_FQ_NAME = FqName("kotlinx.serialization.Serializable")
    private val SERIALIZABLE_CLASS_ID = ClassId.topLevel(SERIALIZABLE_FQ_NAME)

    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirTypeAlias) {
        val expandedType = declaration.expandedTypeRef.coneType
        val expandedTypeSymbol = expandedType.lookupTag.toSymbol(context.session) as? FirClassSymbol<*> ?: return

        // Check if the expanded type is kotlinx.serialization.Serializable
        if (expandedTypeSymbol.classId == SERIALIZABLE_CLASS_ID) {
            // Report a warning on the typealias declaration
            reporter.reportOn(
                declaration.source,
                FirSerializationErrors.TYPEALIASED_SERIALIZABLE_ANNOTATION
            )
        }
    }
}