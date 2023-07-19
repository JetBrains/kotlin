/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirFunctionChecker
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import scala.Option
import scala.jdk.javaapi.CollectionConverters.asScala
import viper.silicon.Config
import viper.silicon.logger.MemberSymbExLogger
import viper.silicon.logger.`NoopSymbExLog$`
import viper.silicon.logger.SymbExLogger
import viper.silicon.verifier.DefaultMainVerifier
import viper.silver.cfg.silver.SilverCfg
import viper.silver.reporter.StdIOReporter

object ViperPoweredDeclarationChecker : FirFunctionChecker() {
    override fun check(declaration: FirFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (declaration !is FirContractDescriptionOwner) return
        val contractDescription = declaration.contractDescription as? FirResolvedContractDescription ?: return

        val converter = Converter()
        val config = Config(asScala(emptyList<String>()).toSeq())

        @Suppress("UNCHECKED_CAST")
        val verifier = DefaultMainVerifier(config, StdIOReporter("stdout_reporter", true), `NoopSymbExLog$`.`MODULE$` as SymbExLogger<out MemberSymbExLogger>)

        @Suppress("UNCHECKED_CAST")
        verifier.verify(converter.program, asScala(emptyList<SilverCfg>()).toSeq(), scala.`None$`.`MODULE$` as Option<String>)

        if (contractDescription.effects.isNotEmpty()) {
            reporter.reportOn(declaration.source, PluginErrors.FUNCTION_WITH_UNVERIFIED_CONTRACT, context)
        }
        // Temporary solution so that we can access the name.
        if (declaration is FirSimpleFunction) {
            // TODO: change this to output the generated Viper.
            reporter.reportOn(declaration.source, PluginErrors.VIPER_TEXT, declaration.name.asString(), context)
        }
    }
}
