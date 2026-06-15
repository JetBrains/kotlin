/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.lombok.checkers

import org.jetbrains.kotlin.builtins.StandardNames.EQUALS_NAME
import org.jetbrains.kotlin.builtins.StandardNames.HASHCODE_NAME
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirRegularClassChecker
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.findArgumentByName
import org.jetbrains.kotlin.fir.resolve.getSuperClassSymbolOrAny
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.scopes.processAllProperties
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.CallSuperMode
import org.jetbrains.kotlin.lombok.config.LombokConfigNames.DO_NOT_USE_GETTERS
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.kotlin.findAnnotationOnPropertyOrField
import org.jetbrains.kotlin.lombok.generators.isEqualsAndHashCode
import org.jetbrains.kotlin.lombok.generators.kotlin.isRelevantForConflictsCheck
import org.jetbrains.kotlin.name.StandardClassIds

object FirLombokEqualsAndHashCodeChecker : FirRegularClassChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirRegularClass) {
        val annotationInfo = context.session.lombokService.getEqualsAndHashCode(declaration.symbol) ?: return
        val source = annotationInfo.annotation.source ?: declaration.source ?: return
        val config = context.session.lombokService.config

        val declaredMemberScope = context.session.declaredMemberScope(declaration.symbol, memberRequiredPhase = null)
        if (declaredMemberScope.hasUserDeclaredEqualsOrHashCode()) {
            /**
             * The user has overridden one of `equals`/`hashCode`. Generating only the
             * missing one would silently couple a user-written method with a generated counterpart that
             * may use a different field set, so we refuse to generate either and ask for both or neither.
             */
            reporter.reportOn(source, LombokFirDiagnostics.EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST, context)
        }

        if ((annotationInfo.callSuper ?: config.equalsAndHashCodeCallSuper) == CallSuperMode.Warn &&
            declaration.symbol.getSuperClassSymbolOrAny(context.session).let { it != null && it.classId != StandardClassIds.Any }
        ) {
            /**
             * Mirrors Lombok behavior: when `lombok.equalsAndHashCode.callSuper=warn` is configured and the
             * annotated class has a non-trivial superclass, warn that the generated `equals`/`hashCode` will
             * not chain to it.
             */
            reporter.reportOn(
                source,
                LombokFirDiagnostics.CALL_SUPER_NOT_CALLED,
                "${EQUALS_NAME}/${HASHCODE_NAME}",
                LombokNames.EQUALS_AND_HASH_CODE.shortName(),
                context,
            )
        }

        if (annotationInfo.doNotUseGetters != null) {
            /**
             * `doNotUseGetters` is a Java-specific concept; in Kotlin a property is always accessed through
             * a unified getter. Warn so users know the parameter has no effect on generated code.
             */
            val argSource = annotationInfo.annotation.findArgumentByName(DO_NOT_USE_GETTERS, returnFirstWhenNotFound = false)!!.source
            reporter.reportOn(argSource, LombokFirDiagnostics.DO_NOT_USE_GETTERS_IRRELEVANT, context)
        }

        declaredMemberScope.processAllProperties { variableSymbol ->
            val property = variableSymbol as? FirPropertySymbol ?: return@processAllProperties
            val includeAnnotation = property
                .findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_INCLUDE_ID, context.session)
                ?: return@processAllProperties
            property.findAnnotationOnPropertyOrField(LombokNames.EQUALS_AND_HASH_CODE_EXCLUDE_ID, context.session)
                ?: return@processAllProperties
            val includeSource = includeAnnotation.source ?: return@processAllProperties
            reporter.reportOn(
                includeSource,
                LombokFirDiagnostics.EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE,
                LombokNames.EQUALS_AND_HASH_CODE.shortName(),
                context
            )
        }
    }

    private fun FirContainingNamesAwareScope.hasUserDeclaredEqualsOrHashCode(): Boolean {
        var found = false

        processAllFunctions {
            found = found ||
                    !it.origin.isEqualsAndHashCode &&
                    it.isRelevantForConflictsCheck &&
                    (it.name == EQUALS_NAME &&
                            it.valueParameterSymbols.singleOrNull()?.resolvedReturnType?.isNullableAny == true ||
                            it.name == HASHCODE_NAME &&
                            it.valueParameterSymbols.isEmpty())
        }

        return found
    }
}
