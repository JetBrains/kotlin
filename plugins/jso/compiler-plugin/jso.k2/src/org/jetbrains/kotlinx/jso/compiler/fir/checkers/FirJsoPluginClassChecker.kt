/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jso.compiler.fir.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.toRegularClassSymbol
import org.jetbrains.kotlinx.jso.compiler.resolve.JsObjectAnnotations

object FirJsoPluginClassChecker : FirClassChecker() {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            val classSymbol = declaration.symbol as? FirRegularClassSymbol ?: return

            if (classSymbol.hasAnnotation(JsObjectAnnotations.jsSimpleObjectAnnotationClassId, session)) {
                checkJsoAnnotationTargets(classSymbol, reporter)
            } else {
                checkJsoSuperInterfaces(classSymbol, reporter)
            }
        }
    }

    context(CheckerContext)
    private fun checkJsoAnnotationTargets(classSymbol: FirClassSymbol<out FirClass>, reporter: DiagnosticReporter) {
        val classKind = classSymbol.classKind.codeRepresentation ?: error("Unexpected enum entry")

        if (!classSymbol.isEffectivelyExternal(session)) {
            reporter.reportOn(classSymbol.source, FirJsoErrors.NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED, classKind)
            return
        }
        if (!classSymbol.isInterface) {
            reporter.reportOn(classSymbol.source, FirJsoErrors.ONLY_INTERFACES_ARE_SUPPORTED, classKind)
            return
        }
    }

    context(CheckerContext)
    private fun checkJsoSuperInterfaces(classSymbol: FirClassSymbol<out FirClass>, reporter: DiagnosticReporter) {
        classSymbol.resolvedSuperTypeRefs.forEach {
            val superInterface = it.coneType.fullyExpandedType(session)
                .toRegularClassSymbol(session)
                ?.takeIf { it.classKind == ClassKind.INTERFACE } ?: return@forEach

            if (superInterface.hasAnnotation(JsObjectAnnotations.jsSimpleObjectAnnotationClassId, session)) {
                reporter.reportOn(
                    it.source,
                    FirJsoErrors.IMPLEMENTING_OF_JSO_IS_NOT_SUPPORTED,
                    classSymbol.classId.asFqNameString()
                )
            }
        }
    }
}
