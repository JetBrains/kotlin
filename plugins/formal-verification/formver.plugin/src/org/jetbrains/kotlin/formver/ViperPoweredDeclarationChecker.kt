/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.KtDiagnosticFactory1
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.conversion.ProgramConverter
import org.jetbrains.kotlin.formver.viper.ConsistencyError
import org.jetbrains.kotlin.formver.viper.VerificationError
import org.jetbrains.kotlin.formver.viper.Verifier
import org.jetbrains.kotlin.formver.viper.VerifierError
import org.jetbrains.kotlin.formver.viper.ast.Program

private val VerifierError.error: KtDiagnosticFactory1<String>
    get() = when (this) {
        is ConsistencyError -> PluginErrors.VIPER_CONSISTENCY_ERROR
        is VerificationError -> PluginErrors.VIPER_VERIFICATION_ERROR
    }

class ViperPoweredDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :
    FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val programConversionContext = ProgramConverter(session, config)
        programConversionContext.registerForVerification(declaration)
        val program = programConversionContext.program

        getProgramForLogging(program)?.let {
            reporter.reportOn(declaration.source, PluginErrors.VIPER_TEXT, declaration.name.asString(), it.toDebugOutput(), context)
        }

        try {
            val verifier = Verifier()

            val success = verifier.verify(program) {
                reporter.reportOn(declaration.source, it.error, it.msg, context)
            }

            if (!success) {
                reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_UNVERIFIED_CONTRACT, declaration.name.asString(), context)
            }
        } catch (e: Exception) {
            System.err.println("Viper verification failed with an exception.  Viper text:\n${program.toDebugOutput()}\nException: $e")
            throw e
        }
    }


    private fun getProgramForLogging(program: Program): Program? = when (config.logLevel) {
        LogLevel.ONLY_WARNINGS -> null
        LogLevel.SHORT_VIPER_DUMP -> program.toShort()
        LogLevel.FULL_VIPER_DUMP -> program
    }
}
