/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.compiler.plugins.kotlin.k2

import androidx.compose.compiler.plugins.kotlin.ComposeLanguageFeature
import androidx.compose.compiler.plugins.kotlin.supportsComposeFeature
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirErrors
import org.jetbrains.kotlin.fir.declarations.FirDeclarationOrigin
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.getSingleMatchedExpectForActualOrNull
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.util.OperatorNameConventions

object ComposableFunctionChecker : FirFunctionChecker(MppCheckerKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: FirFunction) {
        val isComposable = declaration.hasComposableAnnotation(context.session)

        val overrides = declaration.getDirectOverriddenFunctions(context)
        // Check overrides for mismatched composable annotations
        for (override in overrides) {
            if (override.isComposable(context.session) != isComposable) {
                reporter.reportOn(
                    declaration.source,
                    FirErrors.CONFLICTING_OVERLOADS,
                    listOf(declaration.symbol, override)
                )
            } else if (override.isComposable(context.session) && !override.toScheme().canOverride(declaration.symbol.toScheme())) {
                reporter.reportOn(
                    source = declaration.source,
                    factory = ComposeErrors.COMPOSE_APPLIER_DECLARATION_MISMATCH,
                )
            }
        }

        // Check that `actual` composable declarations have composable expects
        declaration.symbol.getSingleMatchedExpectForActualOrNull()?.let { expectDeclaration ->
            if (expectDeclaration.hasComposableAnnotation(context.session) != isComposable) {
                reporter.reportOn(
                    declaration.source,
                    ComposeErrors.MISMATCHED_COMPOSABLE_IN_EXPECT_ACTUAL
                )
            }
        }

        if (!isComposable) return

        // Composable suspend functions are unsupported
        if (declaration.isSuspend) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSABLE_SUSPEND_FUN)
        }

        // Check that there is a metadata for an override of open function with default parameters and warn if it is missing
        if (overrides.any { it.isOpen && it.valueParameterSymbols.any { it.hasDefaultValue } && it.isMissingCompatMetadata() }) {
            reporter.reportOn(
                declaration.source,
                ComposeErrors.DEPRECATED_OPEN_COMPOSABLE_DEFAULT_PARAMETER_VALUE
            )
        }

        val version = context.languageVersionSettings
        if (
            !version.supportsComposeFeature(ComposeLanguageFeature.DefaultParametersInAbstractFunctions) &&
            declaration.effectiveVisibility.publicApi &&
            declaration.isAbstract
        ) {
            declaration.valueParameters.forEach { parameter ->
                if (parameter.defaultValue != null) {
                    reporter.reportOn(
                        parameter.defaultValue?.source,
                        ComposeErrors.ABSTRACT_COMPOSABLE_DEFAULT_PARAMETER_VALUE,
                        version.languageVersion
                    )
                }
            }
        }

        if (
            !version.supportsComposeFeature(ComposeLanguageFeature.DefaultParametersInOpenFunctions) &&
            declaration.effectiveVisibility.publicApi &&
            declaration.isOpen &&
            declaration.valueParameters.any { it.defaultValue != null }
        ) {
            declaration.valueParameters.forEach { parameter ->
                if (parameter.defaultValue != null) {
                    reporter.reportOn(
                        parameter.defaultValue?.source,
                        ComposeErrors.OPEN_COMPOSABLE_DEFAULT_PARAMETER_VALUE,
                        version.languageVersion
                    )
                }
            }
        }

        // Composable main functions are not allowed.
        if (declaration.symbol.isMain(context.session)) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSABLE_FUN_MAIN)
        }

        // Disallow composable setValue operators
        if (declaration.isOperator &&
            declaration.nameOrSpecialName == OperatorNameConventions.SET_VALUE
        ) {
            reporter.reportOn(declaration.source, ComposeErrors.COMPOSE_INVALID_DELEGATE)
        }
    }
}

@OptIn(SymbolInternals::class)
private fun FirFunctionSymbol<*>.isMissingCompatMetadata(): Boolean =
    origin == FirDeclarationOrigin.Library && fir.composeMetadata?.supportsOpenFunctionsWithDefaultParams() != true