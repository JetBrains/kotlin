/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver

import org.jetbrains.kotlin.diagnostics.DiagnosticReporter
import org.jetbrains.kotlin.diagnostics.reportOn
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.checkers.MppCheckerKind
import org.jetbrains.kotlin.fir.analysis.checkers.context.CheckerContext
import org.jetbrains.kotlin.fir.analysis.checkers.declaration.FirSimpleFunctionChecker
import org.jetbrains.kotlin.fir.contracts.FirResolvedContractDescription
import org.jetbrains.kotlin.fir.declarations.FirContractDescriptionOwner
import org.jetbrains.kotlin.fir.declarations.FirSimpleFunction
import org.jetbrains.kotlin.fir.declarations.hasAnnotation
import org.jetbrains.kotlin.formver.conversion.ProgramConverter
import org.jetbrains.kotlin.formver.embeddings.expression.debug.print
import org.jetbrains.kotlin.formver.reporting.reportVerifierError
import org.jetbrains.kotlin.formver.viper.Verifier
import org.jetbrains.kotlin.formver.viper.mangled
import org.jetbrains.kotlin.formver.viper.ast.Program
import org.jetbrains.kotlin.formver.viper.ast.unwrapOr
import org.jetbrains.kotlin.formver.viper.errors.VerifierError
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

private val FirContractDescriptionOwner.hasContract: Boolean
    get() = when (val description = contractDescription) {
        is FirResolvedContractDescription -> description.effects.isNotEmpty()
        else -> false
    }

private fun TargetsSelection.applicable(declaration: FirSimpleFunction): Boolean = when (this) {
    TargetsSelection.ALL_TARGETS -> true
    TargetsSelection.TARGETS_WITH_CONTRACT -> declaration.hasContract
    TargetsSelection.NO_TARGETS -> false
}

class ViperPoweredDeclarationChecker(private val session: FirSession, private val config: PluginConfiguration) :
    FirSimpleFunctionChecker(MppCheckerKind.Common) {
    override fun check(declaration: FirSimpleFunction, context: CheckerContext, reporter: DiagnosticReporter) {
        if (!config.shouldConvert(declaration)) return
        val errorCollector = ErrorCollector()
        try {
            val programConversionContext = ProgramConverter(session, config, errorCollector)
            programConversionContext.registerForVerification(declaration)
            val program = programConversionContext.program

            getProgramForLogging(program)?.let {
                reporter.reportOn(declaration.source, PluginErrors.VIPER_TEXT, declaration.name.asString(), it.toDebugOutput(), context)
            }

            if (shouldDumpExpEmbeddings(declaration)) {
                for ((name, embedding) in programConversionContext.debugExpEmbeddings) {
                    reporter.reportOn(
                        declaration.source,
                        PluginErrors.EXP_EMBEDDING,
                        name.mangled,
                        embedding.debugTreeView.print(),
                        context
                    )
                }
            }

            val verifier = Verifier()
            val onFailure = { err: VerifierError ->
                val source = err.position.unwrapOr { declaration.source }
                reporter.reportVerifierError(source, err, config.errorStyle, context)
            }

            val consistent = verifier.checkConsistency(program, onFailure)
            // If the Viper program is not consistent, that's our error; we shouldn't surface it to the user as an unverified contract.
            if (!consistent || !config.shouldVerify(declaration)) return

            verifier.verify(program, onFailure)
        } catch (e: Exception) {
            val error = errorCollector.formatErrorWithInfos(e.message ?: "No message provided")
            reporter.reportOn(declaration.source, PluginErrors.INTERNAL_ERROR, error, context)
        }

        errorCollector.forEachMinorError {
            reporter.reportOn(declaration.source, PluginErrors.MINOR_INTERNAL_ERROR, it, context)
        }
    }

    private fun getProgramForLogging(program: Program): Program? = when (config.logLevel) {
        LogLevel.ONLY_WARNINGS -> null
        LogLevel.SHORT_VIPER_DUMP -> program.toShort().withoutPredicates()
        LogLevel.SHORT_VIPER_DUMP_WITH_PREDICATES -> program.toShort()
        LogLevel.FULL_VIPER_DUMP -> program
    }

    private fun getAnnotationId(name: String): ClassId =
        ClassId(FqName.fromSegments(listOf("org", "jetbrains", "kotlin", "formver", "plugin")), Name.identifier(name))

    private val neverConvertId: ClassId = getAnnotationId("NeverConvert")
    private val neverVerifyId: ClassId = getAnnotationId("NeverVerify")
    private val alwaysVerifyId: ClassId = getAnnotationId("AlwaysVerify")
    private val dumpExpEmbeddingsId: ClassId = getAnnotationId("DumpExpEmbeddings")

    private fun PluginConfiguration.shouldConvert(declaration: FirSimpleFunction): Boolean = when {
        declaration.hasAnnotation(neverConvertId, session) -> false
        else -> conversionSelection.applicable(declaration)
    }

    private fun PluginConfiguration.shouldVerify(declaration: FirSimpleFunction): Boolean = when {
        declaration.hasAnnotation(neverConvertId, session) -> false
        declaration.hasAnnotation(neverVerifyId, session) -> false
        declaration.hasAnnotation(alwaysVerifyId, session) -> true
        else -> verificationSelection.applicable(declaration)
    }

    private fun shouldDumpExpEmbeddings(declaration: FirSimpleFunction): Boolean =
        declaration.hasAnnotation(dumpExpEmbeddingsId, session)
}
