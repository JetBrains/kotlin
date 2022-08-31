/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.dataframe

import org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.SourceElementPositioningStrategies
import org.jetbrains.kotlin.diagnostics.error1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.expression.ExpressionCheckers
import org.jetbrains.kotlin.fir.analysis.checkers.expression.FirFunctionCallChecker
import org.jetbrains.kotlin.fir.analysis.extensions.FirAdditionalCheckersExtension
import org.jetbrains.kotlin.fir.dataframe.services.DataFramePluginAnnotationsProvider
import org.jetbrains.kotlin.fir.expressions.FirFunctionCall
import org.jetbrains.kotlin.fir.extensions.FirExpressionResolutionExtension
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrarAdapter
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.FirDiagnosticsDirectives
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerTest
import org.jetbrains.kotlin.test.runners.baseFirDiagnosticTestConfiguration
import org.jetbrains.kotlin.test.services.EnvironmentConfigurator
import org.jetbrains.kotlin.test.services.TestServices

abstract class AbstractResearchTest : AbstractKotlinCompilerTest() {

    override fun TestConfigurationBuilder.configuration() {
        baseFirDiagnosticTestConfiguration()
        defaultDirectives {
            +FirDiagnosticsDirectives.ENABLE_PLUGIN_PHASES
            +FirDiagnosticsDirectives.FIR_DUMP
        }

        useConfigurators(
            ::DataFramePluginAnnotationsProvider,
            { testServices: TestServices ->
                EnvironmentConfigurator1(testServices)
            },
        )

    }

    class EnvironmentConfigurator1(testServices: TestServices) : EnvironmentConfigurator(testServices) {
        override fun CompilerPluginRegistrar.ExtensionStorage.registerCompilerExtensions(
            module: TestModule,
            configuration: CompilerConfiguration
        ) {
            FirExtensionRegistrarAdapter.registerExtension(object : FirExtensionRegistrar() {
                override fun ExtensionRegistrarContext.configurePlugin() {
                    +{ it: FirSession -> AdditionalCheckers(it) }
                    +{ it: FirSession ->
                        Injector(it)
                    }
                }
            })
        }
    }

    class AdditionalCheckers(session: FirSession) : FirAdditionalCheckersExtension(session) {
        override val expressionCheckers: ExpressionCheckers = object : ExpressionCheckers() {
            override val functionCallCheckers: Set<FirFunctionCallChecker> = setOf(FunctionCallChecker)
        }
    }

    object FunctionCallChecker : FirFunctionCallChecker() {
        private val ERROR by error1<KtElement, String>(SourceElementPositioningStrategies.DEFAULT)

        override fun check(expression: FirFunctionCall, context: CheckerContext, reporter: DiagnosticReporter) {
            reporter.reportOn(expression.source, ERROR, "Error text", context)
        }
    }

    class Injector(session: FirSession) : FirExpressionResolutionExtension(session) {
        override fun addNewImplicitReceivers(functionCall: FirFunctionCall): List<ConeKotlinType> {
            return emptyList()
        }
    }
}