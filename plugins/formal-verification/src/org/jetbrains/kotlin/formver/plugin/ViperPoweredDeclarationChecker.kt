/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
@file:Suppress("UNCHECKED_CAST")

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
import viper.silicon.Config
import viper.silicon.logger.MemberSymbExLogger
import viper.silicon.logger.`NoopSymbExLog$`
import viper.silicon.logger.SymbExLogger
import viper.silicon.verifier.DefaultMainVerifier
import viper.silver.cfg.silver.SilverCfg
import viper.silver.reporter.StdIOReporter


class ViperPoweredDeclarationChecker(val session: FirSession) : FirSimpleFunctionChecker() {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        val programConversionContext = ProgramConverter(session)
        programConversionContext.addWithBody(declaration)
        val program = programConversionContext.program

        reporter.reportOn(declaration.source, PluginErrors.VIPER_TEXT, program.toString(), context)

        try {
            val verifier = newVerifier()
            val results = verifier.verify(program, emptySeq<SilverCfg>(), Option.None<String>().toScala())

            var anyErrors = false
            for (result in results) {
                if (result.isFatal) {
                    reporter.reportOn(declaration.source, PluginErrors.VIPER_ERROR, result.toString(), context)
                    anyErrors = true
                }
            }
            if (anyErrors) {
                reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_UNVERIFIED_CONTRACT, context)
            }
        } catch (e: Exception) {
            System.err.println("Viper verification failed with an exception.  Viper text:\n$program\nException: $e")
            throw e
        }
    }


    private fun newVerifier(): DefaultMainVerifier {
        val config = Config(seqOf("--ignoreFile", "dummy.vpr"))
        val verifier = DefaultMainVerifier(
            config,
            StdIOReporter("stdout_reporter", true),
            `NoopSymbExLog$`.`MODULE$` as SymbExLogger<out MemberSymbExLogger>
        )
        verifier.start()
        return verifier
    }
}
