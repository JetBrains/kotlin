/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.jspo.compiler.fir.checkers

import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirClassChecker
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.fir.declarations.utils.isEffectivelyExternal
import org.jetbrains.kotlin.fir.declarations.utils.isInline
import org.jetbrains.kotlin.fir.declarations.utils.isInterface
import org.jetbrains.kotlin.fir.declarations.utils.isMethodOfAny
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.UnexpandedTypeCheck
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.isAny
import org.jetbrains.kotlin.fir.resolve.toRegularClassSymbol
import org.jetbrains.kotlinx.jspo.compiler.resolve.JsPlainObjectsAnnotations

object FirJsPlainObjectsPluginClassChecker : FirClassChecker(MppCheckerKind.Platform) {
    override fun check(declaration: FirClass, context: CheckerContext, reporter: DiagnosticReporter) {
        with(context) {
            val classSymbol = declaration.symbol as? FirRegularClassSymbol ?: return

            if (classSymbol.hasAnnotation(JsPlainObjectsAnnotations.jsPlainObjectAnnotationClassId, session)) {
                checkJsPlainObjectAnnotationTargets(classSymbol, context, reporter)
                checkJsPlainObjectSuperTypes(classSymbol, context, reporter)
                checkJsPlainObjectMembers(classSymbol, context, reporter)
            } else {
                checkJsPlainObjectAsSuperInterface(classSymbol, context, reporter)
            }
        }
    }

    private fun checkJsPlainObjectAnnotationTargets(
        classSymbol: FirClassSymbol<FirClass>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val classKind = classSymbol.classKind.codeRepresentation ?: error("Unexpected enum entry")

        if (!classSymbol.isEffectivelyExternal(context.session)) {
            reporter.reportOn(classSymbol.source, FirJsPlainObjectsErrors.NON_EXTERNAL_DECLARATIONS_NOT_SUPPORTED, classKind, context)
            return
        }
        if (!classSymbol.isInterface) {
            reporter.reportOn(classSymbol.source, FirJsPlainObjectsErrors.ONLY_INTERFACES_ARE_SUPPORTED, classKind, context)
            return
        }
    }

    private fun checkJsPlainObjectMembers(
        classSymbol: FirClassSymbol<FirClass>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        if (!classSymbol.isEffectivelyExternal(context.session) || !classSymbol.isInterface) return
        classSymbol
            .declaredMemberScope(context.session, null)
            .processAllFunctions {
                if (!it.isMethodOfAny && !it.isInline) {
                    reporter.reportOn(it.source, FirJsPlainObjectsErrors.METHODS_ARE_NOT_ALLOWED_INSIDE_JS_PLAIN_OBJECT, context)
                }
            }
    }

    private fun checkJsPlainObjectSuperTypes(
        classSymbol: FirClassSymbol<FirClass>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val session = context.session
        if (!classSymbol.isEffectivelyExternal(session) || !classSymbol.isInterface) return
        classSymbol.resolvedSuperTypeRefs.forEach { superType ->
            val superInterface = superType.coneType
                .takeIf { !it.isAny }
                ?.fullyExpandedType(session)
                ?.toRegularClassSymbol(session) ?: return@forEach

            if (!superInterface.isAllowedToUseAsSuperType(session)) {
                reporter.reportOn(
                    superType.source,
                    FirJsPlainObjectsErrors.JS_PLAIN_OBJECT_CAN_EXTEND_ONLY_OTHER_JS_PLAIN_OBJECTS,
                    classSymbol.classId.asFqNameString(),
                    context
                )
            }
        }
    }

    @OptIn(SymbolInternals::class, UnexpandedTypeCheck::class, DirectDeclarationsAccess::class)
    private fun FirClassSymbol<FirClass>.isAllowedToUseAsSuperType(session: FirSession): Boolean =
        hasAnnotation(JsPlainObjectsAnnotations.jsPlainObjectAnnotationClassId, session) ||
                resolvedSuperTypeRefs.singleOrNull()?.isAny == true && fir.declarations.isEmpty()

    private fun checkJsPlainObjectAsSuperInterface(
        classSymbol: FirClassSymbol<FirClass>,
        context: CheckerContext,
        reporter: DiagnosticReporter,
    ) {
        val session = context.session
        classSymbol.resolvedSuperTypeRefs.forEach {
            val superInterface = it.coneType.fullyExpandedType(session)
                .toRegularClassSymbol(session)
                ?.takeIf { it.classKind == ClassKind.INTERFACE } ?: return@forEach

            if (superInterface.hasAnnotation(JsPlainObjectsAnnotations.jsPlainObjectAnnotationClassId, session)) {
                reporter.reportOn(
                    it.source,
                    FirJsPlainObjectsErrors.IMPLEMENTING_OF_JS_PLAIN_OBJECT_IS_NOT_SUPPORTED,
                    classSymbol.classId.asFqNameString(),
                    context
                )
            }
        }
    }
}
