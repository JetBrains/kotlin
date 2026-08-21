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
import org.jetbrains.kotlin.fir.scopes.FirContainingNamesAwareScope
import org.jetbrains.kotlin.fir.scopes.impl.declaredMemberScope
import org.jetbrains.kotlin.fir.scopes.processAllFunctions
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.types.FirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.isNullableAny
import org.jetbrains.kotlin.fir.types.jvm.FirJavaTypeRef
import org.jetbrains.kotlin.load.java.structure.JavaClass
import org.jetbrains.kotlin.load.java.structure.JavaClassifierType
import org.jetbrains.kotlin.lombok.LombokFirDiagnostics
import org.jetbrains.kotlin.lombok.LombokNames
import org.jetbrains.kotlin.lombok.config.lombokService
import org.jetbrains.kotlin.lombok.generators.isEqualsAndHashCode
import org.jetbrains.kotlin.lombok.generators.isPlainClass
import org.jetbrains.kotlin.lombok.generators.hasReceiverOrContextParameters

object FirLombokEqualsAndHashCodeChecker : FirRegularClassChecker(MppCheckerKind.Platform) {
    private val functionNames = setOf(EQUALS_NAME, HASHCODE_NAME)

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
        } else if (declaration.symbol.isPlainClass) {
            /**
             * Mirrors javac's reaction to the members Lombok generates here: "equals(Object) in Child cannot
             * override equals(Object) in Parent; overridden method is final". Only relevant when nothing is
             * declared in the class itself, otherwise nothing is generated at all.
             *
             * The `isPlainClass` guard is the generator's own condition: for anything else nothing is generated
             * and `ANNOTATION_HAS_NO_EFFECT` says so, and an enum in particular inherits a final
             * `equals`/`hashCode` from `java.lang.Enum` (KT-88507) - a second error there would be pointless.
             */
            declaration.findSuperclassWithFinalEqualsOrHashCode()?.let { superClassSymbol ->
                reporter.reportOn(
                    source,
                    LombokFirDiagnostics.EQUALS_OR_HASH_CODE_FUNCTIONS_ARE_FINAL_IN_SUPERCLASS,
                    superClassSymbol.name,
                )
            }
        }

        checkCallSuper(
            annotationInfo.callSuper ?: config.equalsAndHashCodeCallSuper,
            annotationInfo,
            declaration,
            functionNames,
        )

        checkIncludeAndExcludeAnnotations(declaredMemberScope, LombokNames.EQUALS_AND_HASH_CODE_ID)
    }

    /** The closest superclass declaring a final `equals`/`hashCode` that the generated ones would override. */
    context(context: CheckerContext)
    private fun FirRegularClass.findSuperclassWithFinalEqualsOrHashCode(): FirRegularClassSymbol? =
        findSuperclassWithFinalFunction(functionNames) { it.hasGeneratedEqualsOrHashCodeShape }

    private fun FirContainingNamesAwareScope.hasUserDeclaredEqualsOrHashCode(): Boolean {
        var found = false

        processAllFunctions {
            found = found || !it.origin.isEqualsAndHashCode && it.hasGeneratedEqualsOrHashCodeShape
        }

        return found
    }

    /**
     * Whether [this] has the signature of one of the members `@EqualsAndHashCode` generates: `equals(Any?)` or a
     * parameterless `hashCode()`, neither with a receiver or context parameters. Anything else merely shares a
     * name and would not be overridden by what the generator adds.
     */
    private val FirNamedFunctionSymbol.hasGeneratedEqualsOrHashCodeShape: Boolean
        get() = !hasReceiverOrContextParameters &&
                (name == EQUALS_NAME && valueParameterSymbols.singleOrNull()?.isAnyOrJavaObjectType == true ||
                        name == HASHCODE_NAME && valueParameterSymbols.isEmpty())

    /**
     * Whether [this] parameter's type is `Any?`, or `java.lang.Object` for a Java declaration.
     *
     * A Java parameter's type is still a [FirJavaTypeRef] when the declaring class is a supertype the checker only
     * peeks into - signature enhancement has not run for it - so `resolvedReturnTypeRef` would throw and the type
     * has to be matched structurally instead.
     */
    @OptIn(SymbolInternals::class)
    private val FirValueParameterSymbol.isAnyOrJavaObjectType: Boolean
        get() = when (val typeRef = fir.returnTypeRef) {
            is FirResolvedTypeRef -> typeRef.coneType.isNullableAny
            is FirJavaTypeRef -> ((typeRef.type as? JavaClassifierType)?.classifier as? JavaClass)?.fqName ==
                    LombokNames.JAVA_OBJECT_ID.asSingleFqName()
            else -> false
        }
}
