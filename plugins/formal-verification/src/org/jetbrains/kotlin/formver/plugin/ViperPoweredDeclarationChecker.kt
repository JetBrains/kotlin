/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.formver.conversion.ProgramConverter
import org.jetbrains.kotlin.formver.scala.Option
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.Program
import viper.silicon.Config
import viper.silicon.logger.MemberSymbExLogger
import viper.silicon.logger.`NoopSymbExLog$`
import viper.silicon.logger.SymbExLogger
import viper.silicon.verifier.DefaultMainVerifier
import viper.silver.cfg.silver.SilverCfg
import viper.silver.reporter.StdIOReporter

class ViperPoweredDeclarationChecker(private val session: FirSession, private val logLevel: LogLevel) : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val programConversionContext = ProgramConverter(session)
        programConversionContext.addWithBody(declaration)
        val program = programConversionContext.program

        getProgramForLogging(program)?.let {
            reporter.reportOn(declaration.source, PluginErrors.VIPER_TEXT, declaration.name.asString(), it.toViper().toString(), context)
        }

        val viperProgram = program.toViper()

        try {
            var anyErrors = false

            val consistencyResults = viperProgram.checkTransitively()
            for (result in consistencyResults) {
                reporter.reportOn(declaration.source, PluginErrors.VIPER_CONSISTENCY_ERROR, result.toString(), context)
                anyErrors = true
            }

            if (!anyErrors) {
                val verifier = newVerifier()
                val results = verifier.verify(viperProgram, emptySeq<SilverCfg>(), Option.None<String>().toScala())

                for (result in results) {
                    if (result.isFatal) {
                        reporter.reportOn(declaration.source, PluginErrors.VIPER_VERIFICATION_ERROR, result.toString(), context)
                        anyErrors = true
                    }
                }
            }

            if (anyErrors) {
                reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_UNVERIFIED_CONTRACT, declaration.name.asString(), context)
            }
        } catch (e: Exception) {
            System.err.println("Viper verification failed with an exception.  Viper text:\n$viperProgram\nException: $e")
            throw e
        }
    }


    @Suppress("UNCHECKED_CAST")
    private fun newVerifier(): DefaultMainVerifier {
        val config = Config(seqOf("--ignoreFile", "dummy.vpr"))
        val verifier = DefaultMainVerifier(
            config,
            StdIOReporter("stdout_reporter", true),
            `NoopSymbExLog$`.`MODULE$` as SymbExLogger<out MemberSymbExLogger>,
        )
        verifier.start()
        return verifier
    }

    private fun getProgramForLogging(program: Program): Program? = when (logLevel) {
        LogLevel.ONLY_WARNINGS -> null
        LogLevel.SHORT_VIPER_DUMP -> program.toShort()
        LogLevel.FULL_VIPER_DUMP -> program
    }
}
